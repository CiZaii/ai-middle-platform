package com.ai.middle.platform.service.impl;

import com.ai.middle.platform.common.enums.ProcessingStatus;
import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.common.util.FileDetailAttrUtils;
import com.ai.middle.platform.common.util.IdGenerator;
import com.ai.middle.platform.common.util.IdUtil;
import com.ai.middle.platform.config.RabbitMQConfig;
import com.ai.middle.platform.dto.response.FileDTO;
import com.ai.middle.platform.dto.response.FileDetailDTO;
import com.ai.middle.platform.dto.response.FileStatusesDTO;
import com.ai.middle.platform.dto.response.FileUploadResponse;
import com.ai.middle.platform.dto.response.KnowledgeGraphDTO;
import com.ai.middle.platform.dto.response.QaPairDTO;
import com.ai.middle.platform.dto.response.UserDTO;
import com.ai.middle.platform.entity.graph.DocumentNode;
import com.ai.middle.platform.entity.graph.EntityNode;
import com.ai.middle.platform.entity.po.FileDetail;
import com.ai.middle.platform.entity.po.FileDetailAttributes;
import com.ai.middle.platform.entity.po.KbDocument;
import com.ai.middle.platform.entity.po.KbKnowledgeBase;
import com.ai.middle.platform.entity.po.KbQaPair;
import com.ai.middle.platform.entity.po.SysUser;
import com.ai.middle.platform.mq.message.KgGenerationTaskMessage;
import com.ai.middle.platform.mq.message.OcrTaskMessage;
import com.ai.middle.platform.mq.message.QaGenerationTaskMessage;
import com.ai.middle.platform.repository.mapper.FileDetailMapper;
import com.ai.middle.platform.repository.mapper.KbDocumentMapper;
import com.ai.middle.platform.repository.mapper.KbKnowledgeBaseMapper;
import com.ai.middle.platform.repository.mapper.KbQaPairMapper;
import com.ai.middle.platform.repository.mapper.SysUserMapper;
import com.ai.middle.platform.repository.neo4j.DocumentNodeRepository;
import com.ai.middle.platform.repository.neo4j.EntityNodeRepository;
import com.ai.middle.platform.service.AIProcessService;
import com.ai.middle.platform.service.DocumentProcessService;
import com.ai.middle.platform.service.FileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final String OBJECT_TYPE_KB = "knowledge_base";
    private static final String OBJECT_TYPE_DOCUMENT_PAGE = "kb_document_page";

    private final FileDetailMapper fileDetailMapper;
    private final KbDocumentMapper documentMapper;
    private final KbQaPairMapper qaPairMapper;
    private final SysUserMapper userMapper;
    private final KbKnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentNodeRepository documentNodeRepository;
    private final EntityNodeRepository entityNodeRepository;
    private final Neo4jClient neo4jClient;
    private final RabbitTemplate rabbitTemplate;
    private final FileStorageService fileStorageService;
    private final DocumentProcessService documentProcessService;
    private final AIProcessService aiProcessService;

    @Value("${app.file.max-size}")
    private Long maxFileSize;

    @Value("${app.file.allowed-types}")
    private String allowedTypes;

    @Override
    public List<FileDTO> listByKbId(String kbId) {
        KbKnowledgeBase knowledgeBase = findKnowledgeBase(kbId);
        List<FileDetail> files = fileDetailMapper.selectList(new LambdaQueryWrapper<FileDetail>()
                .eq(FileDetail::getObjectType, OBJECT_TYPE_KB)
                .eq(FileDetail::getObjectId, String.valueOf(knowledgeBase.getId()))
                .orderByDesc(FileDetail::getCreateTime));

        return files.stream()
                .map(item -> convertToDTO(item, knowledgeBase))
                .collect(Collectors.toList());
    }

    @Override
    public FileDetailDTO getDetailById(String id) {
        FileDetail file = fileDetailMapper.selectById(id);

        if (file == null || !OBJECT_TYPE_KB.equals(file.getObjectType())) {
            throw new BusinessException("文件不存在");
        }

        Long kbInternalId = parseLong(file.getObjectId());
        if (kbInternalId == null) {
            throw new BusinessException("文件未关联知识库");
        }

        KbKnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(kbInternalId);
        if (knowledgeBase == null) {
            throw new BusinessException("文件所属知识库不存在");
        }

        FileDTO baseDTO = convertToDTO(file, knowledgeBase);
        FileDetailDTO detailDTO = new FileDetailDTO();
        detailDTO.setId(baseDTO.getId());
        detailDTO.setKnowledgeBaseId(baseDTO.getKnowledgeBaseId());
        detailDTO.setName(baseDTO.getName());
        detailDTO.setType(baseDTO.getType());
        detailDTO.setMimeType(baseDTO.getMimeType());
        detailDTO.setSize(baseDTO.getSize());
        detailDTO.setUrl(baseDTO.getUrl());
        detailDTO.setThumbnailUrl(baseDTO.getThumbnailUrl());
        detailDTO.setUploadedAt(baseDTO.getUploadedAt());
        detailDTO.setUploadedBy(baseDTO.getUploadedBy());
        detailDTO.setStatuses(baseDTO.getStatuses());
        detailDTO.setErrorMessage(baseDTO.getErrorMessage());
        detailDTO.setOcrContent(loadAggregatedOcrContent(file.getId()));
        detailDTO.setKnowledgeGraph(buildKnowledgeGraph(file.getId()));
        detailDTO.setQaPairs(buildQaPairs(file.getId()));
        return detailDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResponse upload(String kbId, MultipartFile file) {
        Long currentUserId = 1L;

        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException("文件大小超过限制");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException("无法识别的文件名");
        }

        String extension = getFileExtension(originalFilename);
        if (!isAllowedFileType(extension)) {
            throw new BusinessException("不支持的文件类型");
        }

        KbKnowledgeBase knowledgeBase = findKnowledgeBase(kbId);

        String pathPrefix = "kb/" + kbId + "/";
        String saveFilename = IdUtil.simpleUUID() + "." + extension;

        FileInfo fileInfo = fileStorageService.of(file)
                .setPath(pathPrefix)
                .setSaveFilename(saveFilename)
                .upload();

        if (fileInfo == null || !StringUtils.hasText(fileInfo.getUrl())) {
            throw new BusinessException("文件上传失败");
        }
        String fileId = fileInfo.getId();
        FileDetail fileRecord = fileDetailMapper.selectById(fileId);
        if (fileRecord == null) {
            throw new BusinessException("文件记录未找到");
        }

        FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileRecord.getAttr());
        attributes.setUploadedBy(currentUserId);
        attributes.setFileType(getFileType(extension));
        attributes.setOcrStatus(ProcessingStatus.PENDING.getCode());
        attributes.setVectorizationStatus(ProcessingStatus.PENDING.getCode());
        attributes.setQaPairsStatus(ProcessingStatus.PENDING.getCode());
        attributes.setKnowledgeGraphStatus(ProcessingStatus.PENDING.getCode());
        attributes.setErrorMessage(null);

        fileRecord.setFilename(fileInfo.getFilename());
        fileRecord.setOriginalFilename(originalFilename);
        fileRecord.setExt(StringUtils.hasText(extension) ? extension : fileRecord.getExt());
        fileRecord.setContentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : fileRecord.getContentType());
        fileRecord.setSize(file.getSize());
        fileRecord.setObjectType(OBJECT_TYPE_KB);
        fileRecord.setObjectId(String.valueOf(knowledgeBase.getId()));
        fileRecord.setAttr(FileDetailAttrUtils.toJson(attributes));
        fileDetailMapper.updateById(fileRecord);

        updateKnowledgeBaseFileCount(knowledgeBase, 1);

        createDocumentPages(fileRecord, fileInfo.getUrl());

        return FileUploadResponse.builder()
                .fileId(fileId)
                .name(originalFilename)
                .url(fileInfo.getUrl())
                .size(file.getSize())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        FileDetail file = fileDetailMapper.selectById(id);

        if (file == null || !OBJECT_TYPE_KB.equals(file.getObjectType())) {
            throw new BusinessException("文件不存在");
        }

        fileDetailMapper.deleteById(file.getId());

        Long kbInternalId = parseLong(file.getObjectId());
        KbKnowledgeBase knowledgeBase = kbInternalId == null ? null : knowledgeBaseMapper.selectById(kbInternalId);
        if (knowledgeBase != null) {
            updateKnowledgeBaseFileCount(knowledgeBase, -1);
        }

        cleanupExistingDocumentImages(id);
        documentNodeRepository.deleteDocumentWithRelations(id);
    }

    @Override
    public void triggerProcess(String fileId, String processType) {
        FileDetail file = fileDetailMapper.selectById(fileId);

        if (file == null || !OBJECT_TYPE_KB.equals(file.getObjectType())) {
            throw new BusinessException("文件不存在");
        }

        switch (processType.toLowerCase(Locale.ROOT)) {
            case "ocr":
                restartOcrProcessing(file);
                break;
            case "vectorization":
                log.info("暂未实现的处理类型: vectorization");
                break;
            case "qa-pairs":
                triggerQaPairsProcessing(file);
                break;
            case "knowledge-graph":
                triggerKnowledgeGraphProcessing(file);
                break;
            case "tags":
                triggerTagsGeneration(file);
                break;
            default:
                throw new BusinessException("不支持的处理类型");
        }
    }

    private void restartOcrProcessing(FileDetail file) {
        cleanupExistingDocumentImages(file.getId());

        FileDetailAttributes attributes = FileDetailAttrUtils.parse(file.getAttr());
        attributes.setOcrStatus(ProcessingStatus.PENDING.getCode());
        attributes.setVectorizationStatus(ProcessingStatus.PENDING.getCode());
        attributes.setQaPairsStatus(ProcessingStatus.PENDING.getCode());
        attributes.setKnowledgeGraphStatus(ProcessingStatus.PENDING.getCode());
        attributes.setErrorMessage(null);
        file.setAttr(FileDetailAttrUtils.toJson(attributes));
        fileDetailMapper.updateById(file);

        createDocumentPages(file, file.getUrl());
    }

    private void triggerKnowledgeGraphProcessing(FileDetail file) {
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(file.getAttr());
        if (!ProcessingStatus.COMPLETED.getCode().equals(attributes.getOcrStatus())) {
            throw new BusinessException("请先完成OCR识别后再生成知识图谱");
        }

        attributes.setKnowledgeGraphStatus(ProcessingStatus.PENDING.getCode());
        attributes.setErrorMessage(null);
        file.setAttr(FileDetailAttrUtils.toJson(attributes));
        fileDetailMapper.updateById(file);

        String aggregatedContent = loadAggregatedOcrContent(file.getId());
        if (!StringUtils.hasText(aggregatedContent)) {
            throw new BusinessException("未找到可用于知识图谱生成的内容，请先完成OCR");
        }

        KgGenerationTaskMessage taskMessage = KgGenerationTaskMessage.builder()
                .taskId(IdGenerator.simpleUUID())
                .fileId(file.getId())
                .ocrContent(aggregatedContent)
                .extractEntities(Boolean.TRUE)
                .extractRelations(Boolean.TRUE)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_KG,
                RabbitMQConfig.ROUTING_KEY_KG,
                taskMessage
        );

        log.info("手动触发知识图谱生成任务: fileId={} contentLength={}",
                file.getId(),
                aggregatedContent.length());
    }

    private void triggerQaPairsProcessing(FileDetail file) {
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(file.getAttr());
        if (!ProcessingStatus.COMPLETED.getCode().equals(attributes.getOcrStatus())) {
            throw new BusinessException("请先完成OCR识别后再生成问答对");
        }

        attributes.setQaPairsStatus(ProcessingStatus.PENDING.getCode());
        attributes.setErrorMessage(null);
        file.setAttr(FileDetailAttrUtils.toJson(attributes));
        fileDetailMapper.updateById(file);

        String aggregatedContent = loadAggregatedOcrContent(file.getId());
        if (!StringUtils.hasText(aggregatedContent)) {
            throw new BusinessException("未找到可用于问答对生成的内容，请先完成OCR");
        }

        QaGenerationTaskMessage taskMessage = QaGenerationTaskMessage.builder()
                .taskId(IdGenerator.simpleUUID())
                .fileId(file.getId())
                .ocrContent(aggregatedContent)
                .maxPairs(100)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_QA,
                RabbitMQConfig.ROUTING_KEY_QA,
                taskMessage
        );

        log.info("手动触发问答对生成任务: fileId={} contentLength={}",
                file.getId(),
                aggregatedContent.length());
    }

    private void triggerTagsGeneration(FileDetail file) {
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(file.getAttr());
        if (!ProcessingStatus.COMPLETED.getCode().equals(attributes.getOcrStatus())) {
            throw new BusinessException("请先完成OCR识别后再生成标签");
        }

        String aggregatedContent = loadAggregatedOcrContent(file.getId());
        if (!StringUtils.hasText(aggregatedContent)) {
            throw new BusinessException("未找到可用于标签生成的内容，请先完成OCR");
        }

        String fileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : file.getFilename();

        // 同步调用AI生成标签
        List<String> tags = aiProcessService.generateTags(file.getId(), fileName, aggregatedContent);

        // 更新文件属性中的标签
        attributes.setTags(tags);
        file.setAttr(FileDetailAttrUtils.toJson(attributes));
        fileDetailMapper.updateById(file);

        log.info("标签生成完成并已保存: fileId={} tags={}", file.getId(), tags);
    }

    private void cleanupExistingDocumentImages(String fileId) {
        List<FileDetail> pageFiles = fileDetailMapper.selectList(new LambdaQueryWrapper<FileDetail>()
                .eq(FileDetail::getObjectType, OBJECT_TYPE_DOCUMENT_PAGE)
                .eq(FileDetail::getObjectId, fileId));

        List<KbDocument> existingDocuments = documentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, fileId));

        if (!pageFiles.isEmpty()) {
            for (FileDetail pageFile : pageFiles) {
                deleteRemoteFileQuietly(pageFile);
            }

            List<String> pageFileIds = pageFiles.stream()
                    .map(FileDetail::getId)
                    .filter(StringUtils::hasText)
                    .toList();
            if (!pageFileIds.isEmpty()) {
                fileDetailMapper.deleteBatchIds(pageFileIds);
            }

            if (!existingDocuments.isEmpty()) {
                Set<String> knownUrls = pageFiles.stream()
                        .map(FileDetail::getUrl)
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .filter(url -> !url.isEmpty())
                        .collect(Collectors.toSet());

                for (KbDocument document : existingDocuments) {
                    String url = document.getImageUrl();
                    if (!StringUtils.hasText(url)) {
                        continue;
                    }
                    String trimmed = url.trim();
                    if (trimmed.isEmpty() || knownUrls.contains(trimmed)) {
                        continue;
                    }
                    deleteRemoteFileByUrl(trimmed);
                }
            }
        } else if (!existingDocuments.isEmpty()) {
            for (KbDocument document : existingDocuments) {
                String url = document.getImageUrl();
                if (!StringUtils.hasText(url)) {
                    continue;
                }
                deleteRemoteFileByUrl(url.trim());
            }
        }

        if (!existingDocuments.isEmpty()) {
            documentMapper.delete(new LambdaQueryWrapper<KbDocument>()
                    .eq(KbDocument::getFileId, fileId));
        }
    }

    private void deleteRemoteFileQuietly(FileDetail fileDetail) {
        if (fileDetail == null || !StringUtils.hasText(fileDetail.getUrl())) {
            return;
        }
        String url = fileDetail.getUrl().trim();
        if (url.isEmpty()) {
            return;
        }
        try {
            FileInfo fileInfo = fileStorageService.getFileInfoByUrl(url);
            boolean deleted = fileInfo != null
                    ? fileStorageService.delete(fileInfo)
                    : fileStorageService.delete(url);
            if (!deleted) {
                log.warn("Failed to delete OCR page image from storage: {}", url);
            }
        } catch (Exception ex) {
            log.warn("Error deleting OCR page image from storage: {}", url, ex);
        }
    }

    private void deleteRemoteFileByUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        try {
            FileInfo fileInfo = fileStorageService.getFileInfoByUrl(trimmed);
            boolean deleted = fileInfo != null
                    ? fileStorageService.delete(fileInfo)
                    : fileStorageService.delete(trimmed);
            if (!deleted) {
                log.warn("Failed to delete OCR page image from storage: {}", trimmed);
            }
        } catch (Exception ex) {
            log.warn("Error deleting OCR page image from storage: {}", trimmed, ex);
        }
    }

    private void createDocumentPages(FileDetail fileDetail, String fileUrl) {
        documentMapper.delete(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, fileDetail.getId()));

        List<String> pageImageUrls = new ArrayList<>();
        File downloadedFile = null;
        try {
            byte[] bytes = fileStorageService.download(fileUrl).bytes();
            downloadedFile = createTempFileFromBytes(bytes, fileDetail.getOriginalFilename());
            FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileDetail.getAttr());
            String fileType = attributes.getFileType();
            if ("word".equalsIgnoreCase(fileType)) {
                String pdfPath = documentProcessService.convertWordToPdf(downloadedFile);
                pageImageUrls = documentProcessService.splitPdfToImages(pdfPath, fileDetail.getId());
                File pdfFile = new File(pdfPath);
                deleteTempFile(pdfFile);
            } else if ("pdf".equalsIgnoreCase(fileType)) {
                pageImageUrls = documentProcessService.splitPdfToImages(downloadedFile.getAbsolutePath(), fileDetail.getId());
            } else if ("image".equalsIgnoreCase(fileType)) {
                String imageUrl = documentProcessService.processImageFile(downloadedFile.getAbsolutePath(), fileDetail.getId());
                pageImageUrls.add(imageUrl);
            } else {
                throw new BusinessException("Unsupported file type for OCR: " + fileType);
            }

            if (pageImageUrls.isEmpty()) {
                throw new BusinessException("未能提取有效的页面图像");
            }

            for (int i = 0; i < pageImageUrls.size(); i++) {
                int pageIndex = i + 1;
                KbDocument document = KbDocument.builder()
                        .documentId(IdGenerator.simpleUUID())
                        .fileId(fileDetail.getId())
                        .pageIndex(pageIndex)
                        .imageUrl(pageImageUrls.get(i))
                        .ocrStatus(ProcessingStatus.PENDING.getCode())
                        .tokensUsed(0)
                        .build();
                documentMapper.insert(document);
                sendOcrTaskForPage(fileDetail, document);
            }

            log.info("Created {} documents for OCR processing: fileId={}", pageImageUrls.size(), fileDetail.getId());
        } finally {
            deleteTempFile(downloadedFile);
        }
    }

    private File createTempFileFromBytes(byte[] data, String originalFilename) {
        if (data == null || data.length == 0) {
            throw new BusinessException("下载文件内容为空");
        }
        try {
            String suffix = ".tmp";
            if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            Path tempFile = Files.createTempFile("kb_download_", suffix);
            Files.write(tempFile, data);
            return tempFile.toFile();
        } catch (IOException e) {
            throw new BusinessException("Failed to create temp file: " + e.getMessage());
        }
    }

    private void sendOcrTaskForPage(FileDetail file, KbDocument document) {
        OcrTaskMessage message = OcrTaskMessage.builder()
                .taskId(IdGenerator.simpleUUID())
                .fileId(file.getId())
                .documentId(document.getDocumentId())
                .pageIndex(document.getPageIndex())
                .imageUrl(document.getImageUrl())
                .fileType("image")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_OCR,
                RabbitMQConfig.ROUTING_KEY_OCR,
                message
        );

        log.info("发送单页OCR任务: fileId={} pageIndex={}", file.getId(), document.getPageIndex());
    }

    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file.toPath());
        } catch (Exception ex) {
            log.warn("Failed to delete temp file: {}", file.getAbsolutePath(), ex);
        }
    }

    private String loadAggregatedOcrContent(String fileId) {
        LambdaQueryWrapper<KbDocument> documentQuery = new LambdaQueryWrapper<>();
        documentQuery.eq(KbDocument::getFileId, fileId);
        documentQuery.orderByAsc(KbDocument::getPageIndex);
        List<KbDocument> documents = documentMapper.selectList(documentQuery);

        return documents.stream()
                .map(KbDocument::getContent)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
    }

    private KnowledgeGraphDTO buildKnowledgeGraph(String fileId) {
        DocumentNode documentNode = documentNodeRepository.findByFileId(fileId);
        if (documentNode == null) {
            return null;
        }

        List<KnowledgeGraphDTO.GraphNodeDTO> nodes = new ArrayList<>();
        List<KnowledgeGraphDTO.GraphEdgeDTO> edges = new ArrayList<>();
        nodes.add(KnowledgeGraphDTO.GraphNodeDTO.builder()
                .id(documentNode.getId())
                .label(documentNode.getName())
                .type("document")
                .build());

        Set<String> processedEntityIds = new HashSet<>();
        List<Object> rawEntities = documentNodeRepository.findEntitiesByDocumentId(fileId);
        for (Object rawEntity : rawEntities) {
            EntityNode entityNode = extractEntityNode(rawEntity);
            if (entityNode == null || processedEntityIds.contains(entityNode.getId())) {
                continue;
            }

            processedEntityIds.add(entityNode.getId());
            nodes.add(KnowledgeGraphDTO.GraphNodeDTO.builder()
                    .id(entityNode.getId())
                    .label(entityNode.getName())
                    .type(entityNode.getType())
                    .build());
            String relationLabel = StringUtils.hasText(entityNode.getRelationType())
                    ? entityNode.getRelationType()
                    : "BELONGS_TO";
            edges.add(KnowledgeGraphDTO.GraphEdgeDTO.builder()
                    .source(documentNode.getId())
                    .target(entityNode.getId())
                    .label(relationLabel)
                    .build());
        }

        if (!processedEntityIds.isEmpty()) {
            Set<String> relationshipEdgeKeys = new HashSet<>();
            Collection<Map<String, Object>> relations = neo4jClient.query("""
                    MATCH (source:Entity)-[rel]->(target:Entity)
                    WHERE source.attributes.documentId = $documentId
                      AND target.attributes.documentId = $documentId
                      AND type(rel) <> 'BELONGS_TO'
                    RETURN source.id AS sourceId,
                           target.id AS targetId,
                           type(rel) AS relationType,
                           rel.description AS description
                    """)
                    .bind(fileId).to("documentId")
                    .fetch().all();

            for (Map<String, Object> relation : relations) {
                String sourceId = toStringValue(relation.get("sourceId"));
                String targetId = toStringValue(relation.get("targetId"));
                if (!StringUtils.hasText(sourceId) || !StringUtils.hasText(targetId)) {
                    continue;
                }
                if (!processedEntityIds.contains(sourceId) || !processedEntityIds.contains(targetId)) {
                    continue;
                }

                String relationType = toStringValue(relation.get("relationType"));
                String edgeKey = sourceId + "->" + targetId + "::" + relationType;
                if (!relationshipEdgeKeys.add(edgeKey)) {
                    continue;
                }

                String label = StringUtils.hasText(relationType) ? relationType : "RELATED_TO";
                edges.add(KnowledgeGraphDTO.GraphEdgeDTO.builder()
                        .source(sourceId)
                        .target(targetId)
                        .label(label)
                        .build());
            }
        }

        return new KnowledgeGraphDTO(nodes, edges);
    }

    private EntityNode extractEntityNode(Object rawEntity) {
        if (rawEntity instanceof EntityNode entityNode) {
            return entityNode;
        }

        if (rawEntity instanceof Map<?, ?> map) {
            EntityNode entityNode = new EntityNode();
            entityNode.setId(toStringValue(map.get("id")));
            entityNode.setName(toStringValue(map.get("name")));
            entityNode.setType(toStringValue(map.get("type")));
            entityNode.setRelationType(toStringValue(map.get("relationType")));
            return entityNode;
        }
        return null;
    }

    private List<QaPairDTO> buildQaPairs(String fileId) {
        LambdaQueryWrapper<KbQaPair> pairQuery = new LambdaQueryWrapper<>();
        pairQuery.eq(KbQaPair::getFileId, fileId);
        pairQuery.orderByAsc(KbQaPair::getCreatedAt);
        List<KbQaPair> pairs = qaPairMapper.selectList(pairQuery);

        return pairs.stream()
                .map(pair -> QaPairDTO.builder()
                        .question(pair.getQuestion())
                        .answer(pair.getAnswer())
                        .sourceText(pair.getSourceText())
                        .build())
                .collect(Collectors.toList());
    }

    private FileDTO convertToDTO(FileDetail file, KbKnowledgeBase knowledgeBase) {
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(file.getAttr());

        FileStatusesDTO statuses = new FileStatusesDTO();
        statuses.setOcr(mapProcessingStatus(attributes.getOcrStatus()));
        statuses.setVectorization(mapProcessingStatus(attributes.getVectorizationStatus()));
        statuses.setQaPairs(mapProcessingStatus(attributes.getQaPairsStatus()));
        statuses.setKnowledgeGraph(mapProcessingStatus(attributes.getKnowledgeGraphStatus()));

        UserDTO uploadedBy = null;
        if (attributes.getUploadedBy() != null) {
            SysUser uploader = userMapper.selectById(attributes.getUploadedBy());
            if (uploader != null) {
                uploadedBy = UserDTO.builder()
                        .id(uploader.getId())
                        .username(uploader.getUsername())
                        .name(uploader.getName())
                        .email(uploader.getEmail())
                        .avatar(uploader.getAvatar())
                        .role(uploader.getRole())
                        .build();
            }
        }

        String displayName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : file.getFilename();
        String fileType = StringUtils.hasText(attributes.getFileType())
                ? attributes.getFileType()
                : getFileType(file.getExt());

        return FileDTO.builder()
                .id(file.getId())
                .knowledgeBaseId(knowledgeBase.getKbId())
                .name(displayName)
                .type(fileType)
                .mimeType(file.getContentType())
                .size(file.getSize())
                .uploadedAt(file.getCreateTime())
                .uploadedBy(uploadedBy)
                .statuses(statuses)
                .url(file.getUrl())
                .thumbnailUrl(file.getThUrl())
                .tags(attributes.getTags())
                .errorMessage(attributes.getErrorMessage())
                .build();
    }

    private String mapProcessingStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return ProcessingStatus.PENDING.getCode();
        }
        for (ProcessingStatus processingStatus : ProcessingStatus.values()) {
            if (processingStatus.getCode().equalsIgnoreCase(status)) {
                return processingStatus.getCode();
            }
        }
        return ProcessingStatus.PENDING.getCode();
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            log.warn("无法解析文件关联的知识库ID: {}", value);
            return null;
        }
    }

    private String toStringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private KbKnowledgeBase findKnowledgeBase(String kbId) {
        KbKnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KbKnowledgeBase>()
                .eq(KbKnowledgeBase::getKbId, kbId));
        if (knowledgeBase == null) {
            throw new BusinessException("知识库不存在: " + kbId);
        }
        return knowledgeBase;
    }

    private String getFileExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedFileType(String extension) {
        if (!StringUtils.hasText(allowedTypes)) {
            return true;
        }
        String[] types = allowedTypes.split(",");
        for (String type : types) {
            if (type.trim().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    private String getFileType(String extension) {
        return switch (extension) {
            case "pdf" -> "pdf";
            case "jpg", "jpeg", "png", "bmp", "gif" -> "image";
            case "doc", "docx" -> "word";
            default -> "other";
        };
    }

    private void updateKnowledgeBaseFileCount(KbKnowledgeBase knowledgeBase, int delta) {
        int newCount = Math.max(0, (knowledgeBase.getFileCount() == null ? 0 : knowledgeBase.getFileCount()) + delta);
        knowledgeBase.setFileCount(newCount);
        knowledgeBaseMapper.updateById(knowledgeBase);
    }

    private List<String> buildPageImageUrls(List<KbDocument> documents) {
        return documents.stream()
                .map(KbDocument::getImageUrl)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileDTO> searchByTag(String kbId, String tag) {
        if (!StringUtils.hasText(tag)) {
            return List.of();
        }

        KbKnowledgeBase knowledgeBase = findKnowledgeBase(kbId);

        // 查询该知识库下的所有文件
        List<FileDetail> allFiles = fileDetailMapper.selectList(new LambdaQueryWrapper<FileDetail>()
                .eq(FileDetail::getObjectType, OBJECT_TYPE_KB)
                .eq(FileDetail::getObjectId, String.valueOf(knowledgeBase.getId()))
                .orderByDesc(FileDetail::getCreateTime));

        String searchTag = tag.trim().toLowerCase(Locale.ROOT);

        // 在内存中过滤包含该标签的文件
        return allFiles.stream()
                .filter(file -> {
                    FileDetailAttributes attributes = FileDetailAttrUtils.parse(file.getAttr());
                    List<String> tags = attributes.getTags();
                    if (tags == null || tags.isEmpty()) {
                        return false;
                    }
                    // 检查是否包含该标签（不区分大小写）
                    return tags.stream()
                            .anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(searchTag));
                })
                .map(file -> convertToDTO(file, knowledgeBase))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOcrContent(String fileId, String content) {
        FileDetail file = fileDetailMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 查询所有相关的文档页面
        List<KbDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, fileId)
                .orderByAsc(KbDocument::getPageIndex));

        if (documents.isEmpty()) {
            // 如果没有文档记录，创建一个新的
            KbDocument newDocument = new KbDocument();
            newDocument.setDocumentId(IdGenerator.simpleUUID());
            newDocument.setFileId(fileId);
            newDocument.setPageIndex(0);
            newDocument.setContent(content);
            newDocument.setOcrStatus(ProcessingStatus.COMPLETED.getCode());
            documentMapper.insert(newDocument);
        } else {
            // 更新第一个文档页面的内容
            KbDocument firstDocument = documents.get(0);
            firstDocument.setContent(content);
            firstDocument.setOcrStatus(ProcessingStatus.COMPLETED.getCode());
            documentMapper.updateById(firstDocument);

            // 如果有多个页面，删除其他页面（因为用户编辑的是合并后的内容）
            if (documents.size() > 1) {
                for (int i = 1; i < documents.size(); i++) {
                    documentMapper.deleteById(documents.get(i).getId());
                }
            }
        }

        log.info("更新文件OCR内容: fileId={}, contentLength={}", fileId, content != null ? content.length() : 0);
    }
}
