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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

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

        log.info("开始删除文件及其所有关联数据: fileId={}", id);

        // 1. 删除文档页面记录和图片（OCR相关）
        log.info("删除文档页面和OCR图片: fileId={}", id);
        cleanupExistingDocumentImages(id);
        
        // 删除文档记录
        int docDeleteCount = documentMapper.delete(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, id));
        log.info("删除文档记录: fileId={}, count={}", id, docDeleteCount);

        // 2. 删除Neo4j中的知识图谱数据（文档节点和实体节点）
        log.info("删除Neo4j知识图谱数据: fileId={}", id);
        try {
            // 删除文档节点及其关系
            documentNodeRepository.deleteDocumentWithRelations(id);
            
            // 删除该文件相关的实体节点（如果实体只属于这个文档）
            String deleteEntitiesQuery = 
                "MATCH (e:Entity)-[:BELONGS_TO]->(d:Document {fileId: $fileId}) " +
                "WHERE NOT EXISTS { " +
                "  MATCH (e)-[:BELONGS_TO]->(other:Document) " +
                "  WHERE other.fileId <> $fileId " +
                "} " +
                "DETACH DELETE e";
            neo4jClient.query(deleteEntitiesQuery)
                    .bind(id).to("fileId")
                    .run();
            log.info("删除Neo4j实体节点完成: fileId={}", id);
        } catch (Exception e) {
            log.error("删除Neo4j数据失败: fileId={}", id, e);
        }

        // 3. 删除问答对数据
        log.info("删除问答对数据: fileId={}", id);
        int qaPairDeleteCount = qaPairMapper.delete(new LambdaQueryWrapper<KbQaPair>()
                .eq(KbQaPair::getFileId, id));
        log.info("删除问答对: fileId={}, count={}", id, qaPairDeleteCount);

        // 4. 删除向量化数据（从vector_store表中删除）
        log.info("删除向量化数据: fileId={}", id);
        try {
            String deleteVectorSql = 
                "DELETE FROM vector_store WHERE metadata->>'file_id' = ?";
            int vectorDeleteCount = jdbcTemplate.update(deleteVectorSql, id);
            log.info("删除向量数据: fileId={}, count={}", id, vectorDeleteCount);
        } catch (Exception e) {
            log.error("删除向量数据失败: fileId={}", id, e);
        }

        // 5. 删除MinIO中的原始文件
        log.info("删除MinIO原始文件: fileId={}, url={}", id, file.getUrl());
        try {
            if (StringUtils.hasText(file.getUrl())) {
                fileStorageService.delete(file.getUrl());
                log.info("MinIO文件删除成功: fileId={}", id);
            }
        } catch (Exception e) {
            log.error("删除MinIO文件失败: fileId={}, url={}", id, file.getUrl(), e);
        }

        // 6. 删除缩略图（如果有）
        if (StringUtils.hasText(file.getThUrl())) {
            try {
                fileStorageService.delete(file.getThUrl());
                log.info("缩略图删除成功: fileId={}", id);
            } catch (Exception e) {
                log.error("删除缩略图失败: fileId={}, url={}", id, file.getThUrl(), e);
            }
        }

        // 7. 删除文件记录（包含标签等元数据）
        log.info("删除文件记录: fileId={}", id);
        fileDetailMapper.deleteById(file.getId());

        // 8. 更新知识库文件计数
        Long kbInternalId = parseLong(file.getObjectId());
        KbKnowledgeBase knowledgeBase = kbInternalId == null ? null : knowledgeBaseMapper.selectById(kbInternalId);
        if (knowledgeBase != null) {
            updateKnowledgeBaseFileCount(knowledgeBase, -1);
        }

        log.info("文件删除完成，所有关联数据已清理: fileId={}", id);
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
        // 清理现有文档页面和图片
        cleanupExistingDocumentImages(file.getId());

        // 只重置OCR状态，不影响其他处理状态（向量化、问答对、知识图谱）
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(file.getAttr());
        attributes.setOcrStatus(ProcessingStatus.PENDING.getCode());
        // 不重置其他状态，让它们保持独立
        // attributes.setVectorizationStatus(...);  // 保持不变
        // attributes.setQaPairsStatus(...);         // 保持不变
        // attributes.setKnowledgeGraphStatus(...);  // 保持不变
        attributes.setErrorMessage(null);
        file.setAttr(FileDetailAttrUtils.toJson(attributes));
        fileDetailMapper.updateById(file);

        // 重新创建文档页面并触发OCR
        createDocumentPages(file, file.getUrl());
        
        log.info("重新触发OCR处理，其他状态保持不变: fileId={}", file.getId());
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
        // 查询文档节点
        DocumentNode documentNode = documentNodeRepository.findByFileId(fileId);
        if (documentNode == null) {
            return null;
        }

        List<KnowledgeGraphDTO.GraphNodeDTO> nodes = new ArrayList<>();
        List<KnowledgeGraphDTO.GraphEdgeDTO> edges = new ArrayList<>();
        
        // 不添加文档节点，只添加实体节点
        log.debug("buildKnowledgeGraph: fileId={}, skipping document node", fileId);
        
        // 添加所有实体节点（使用 neo4jClient 直接查询）
        Set<String> processedEntityIds = new HashSet<>();
        Collection<Map<String, Object>> entityMaps = neo4jClient.query("""
                MATCH (d:Document {id: $fileId})<-[:BELONGS_TO]-(e:Entity)
                RETURN e.id AS id, 
                       e.name AS name, 
                       e.type AS type
                """)
                .bind(fileId).to("fileId")
                .fetch().all();
        
        log.info("buildKnowledgeGraph: fileId={}, found {} entities from Neo4j", fileId, entityMaps.size());
        
        for (Map<String, Object> entityMap : entityMaps) {
            String entityId = toStringValue(entityMap.get("id"));
            String entityName = toStringValue(entityMap.get("name"));
            String entityType = toStringValue(entityMap.get("type"));
            
            if (!StringUtils.hasText(entityId)) {
                log.warn("buildKnowledgeGraph: skipping entity with null id, name={}", entityName);
                continue;
            }
            
            if (processedEntityIds.contains(entityId)) {
                log.debug("buildKnowledgeGraph: skipping duplicate entity id={}", entityId);
                continue;
            }

            processedEntityIds.add(entityId);
            nodes.add(KnowledgeGraphDTO.GraphNodeDTO.builder()
                    .id(entityId)
                    .label(entityName)
                    .type(entityType)
                    .build());
            
            // 不添加文档到实体的关系
            
            log.debug("buildKnowledgeGraph: added entity node id={}, name={}, type={}", 
                entityId, entityName, entityType);
        }

        // 查询实体之间的关系（通过BELONGS_TO关系找到属于该文档的实体，然后查询它们之间的关系）
        if (!processedEntityIds.isEmpty()) {
            Set<String> relationshipEdgeKeys = new HashSet<>();
            Collection<Map<String, Object>> relations = neo4jClient.query("""
                    MATCH (d:Document {id: $fileId})<-[:BELONGS_TO]-(source:Entity)
                    MATCH (d)<-[:BELONGS_TO]-(target:Entity)
                    MATCH (source)-[rel]->(target)
                    WHERE type(rel) <> 'BELONGS_TO'
                    RETURN source.id AS sourceId,
                           target.id AS targetId,
                           type(rel) AS relationType,
                           rel.description AS description
                    """)
                    .bind(fileId).to("fileId")
                    .fetch().all();

            log.debug("buildKnowledgeGraph: found {} relationships", relations.size());

            for (Map<String, Object> relation : relations) {
                String sourceId = toStringValue(relation.get("sourceId"));
                String targetId = toStringValue(relation.get("targetId"));
                if (!StringUtils.hasText(sourceId) || !StringUtils.hasText(targetId)) {
                    continue;
                }
                if (!processedEntityIds.contains(sourceId) || !processedEntityIds.contains(targetId)) {
                    log.debug("buildKnowledgeGraph: skipping relation with missing entity: {} -> {}", sourceId, targetId);
                    continue;
                }

                String relationType = toStringValue(relation.get("relationType"));
                String description = toStringValue(relation.get("description"));
                String edgeKey = sourceId + "->" + targetId + "::" + relationType;
                if (!relationshipEdgeKeys.add(edgeKey)) {
                    continue;
                }

                // 优先使用description，如果为空则使用relationType，最后兜底使用"关联"
                String label = StringUtils.hasText(description) ? description : 
                              (StringUtils.hasText(relationType) ? relationType : "关联");
                edges.add(KnowledgeGraphDTO.GraphEdgeDTO.builder()
                        .source(sourceId)
                        .target(targetId)
                        .label(label)
                        .build());
                
                log.debug("buildKnowledgeGraph: added edge {} -[{}]-> {}", sourceId, label, targetId);
            }
        }

        log.info("buildKnowledgeGraph: completed for fileId={}, nodes={} entities, edges={}", 
            fileId, processedEntityIds.size(), edges.size());

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
            entityNode.setExternalId(toStringValue(map.get("externalId")));
            entityNode.setDescription(toStringValue(map.get("description")));
            entityNode.setDocumentId(toStringValue(map.get("documentId")));
            entityNode.setRelationType(toStringValue(map.get("relationType")));
            
            // 处理aliases数组
            Object aliasesObj = map.get("aliases");
            if (aliasesObj instanceof String[]) {
                entityNode.setAliases((String[]) aliasesObj);
            }
            
            // 处理sourcePages数组
            Object sourcePagesObj = map.get("sourcePages");
            if (sourcePagesObj instanceof int[]) {
                entityNode.setSourcePages((int[]) sourcePagesObj);
            } else if (sourcePagesObj instanceof long[]) {
                long[] longArray = (long[]) sourcePagesObj;
                int[] intArray = new int[longArray.length];
                for (int i = 0; i < longArray.length; i++) {
                    intArray[i] = (int) longArray[i];
                }
                entityNode.setSourcePages(intArray);
            }
            
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
