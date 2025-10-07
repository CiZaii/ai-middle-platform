package com.ai.middle.platform.service.impl;

import com.ai.middle.platform.common.enums.ProcessingStatus;
import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.common.util.FileDetailAttrUtils;
import com.ai.middle.platform.common.util.IdGenerator;
import com.ai.middle.platform.entity.graph.DocumentNode;
import com.ai.middle.platform.entity.graph.EntityNode;
import com.ai.middle.platform.entity.po.FileDetail;
import com.ai.middle.platform.entity.po.FileDetailAttributes;
import com.ai.middle.platform.entity.po.KbDocument;
import com.ai.middle.platform.entity.po.KbQaPair;
import com.ai.middle.platform.repository.mapper.FileDetailMapper;
import com.ai.middle.platform.repository.mapper.KbDocumentMapper;
import com.ai.middle.platform.repository.mapper.KbQaPairMapper;
import com.ai.middle.platform.repository.neo4j.DocumentNodeRepository;
import com.ai.middle.platform.repository.neo4j.EntityNodeRepository;
import com.ai.middle.platform.service.AIModelFactory;
import com.ai.middle.platform.service.AIProcessService;
import com.ai.middle.platform.service.ModelConfigService;
import com.ai.middle.platform.service.ModelConfigService.ModelRuntimeConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * AI processing service implementation backed by LangChain4j.
 */
@Slf4j
@Service
public class AIProcessServiceImpl implements AIProcessService {

    private static final int DEFAULT_CHUNK_SIZE = 2000;
    private static final int DEFAULT_OVERLAP = 200;
    private static final int MIN_CHUNK_SIZE = 200;
    
    // 问答对生成配置 - 基于Markdown语义分段
    private static final int MIN_SEGMENT_LENGTH = 100;      // 最小段落长度（字符）
    private static final int IDEAL_SEGMENT_LENGTH = 400;    // 理想段落长度（字符）
    private static final int MAX_SEGMENT_LENGTH = 1200;     // 最大段落长度（字符）
    private static final int DEFAULT_QA_MAX = 100;          // 最多生成问答对数
    private static final int DEFAULT_QA_MIN = 10;           // 最少生成问答对数
    private static final BigDecimal DEFAULT_QA_CONFIDENCE = BigDecimal.valueOf(0.9);
    private static final String BUSINESS_OCR = "ocr";
    private static final String BUSINESS_QA = "qa";
    private static final String BUSINESS_KG = "kg";
    private static final String BUSINESS_TAG = "tag";       // 标签生成业务
    private static final int TAG_CONTENT_PREVIEW_LENGTH = 500;  // 标签生成内容预览长度
    private static final int TAG_COUNT = 5;                  // 生成标签数量
    private static final int KG_PAGES_PER_REQUEST = 3;
    private static final int MAX_KEY_ATTEMPTS = 5;

    private final AIModelFactory aiModelFactory;
    private final ModelConfigService modelConfigService;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final KbQaPairMapper qaPairMapper;
    private final FileDetailMapper fileDetailMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final DocumentNodeRepository documentNodeRepository;
    private final EntityNodeRepository entityNodeRepository;
    private final Neo4jClient neo4jClient;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private final com.ai.middle.platform.service.PromptService promptService;

    public AIProcessServiceImpl(AIModelFactory aiModelFactory,
                                ModelConfigService modelConfigService,
                                EmbeddingStore<TextSegment> embeddingStore,
                                EmbeddingModel embeddingModel,
                                KbQaPairMapper qaPairMapper,
                                FileDetailMapper fileDetailMapper,
                                KbDocumentMapper kbDocumentMapper,
                                DocumentNodeRepository documentNodeRepository,
                                EntityNodeRepository entityNodeRepository,
                                Neo4jClient neo4jClient,
                                ObjectMapper objectMapper,
                                FileStorageService fileStorageService,
                                com.ai.middle.platform.service.PromptService promptService) {
        this.aiModelFactory = aiModelFactory;
        this.modelConfigService = modelConfigService;
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.qaPairMapper = qaPairMapper;
        this.fileDetailMapper = fileDetailMapper;
        this.kbDocumentMapper = kbDocumentMapper;
        this.documentNodeRepository = documentNodeRepository;
        this.entityNodeRepository = entityNodeRepository;
        this.neo4jClient = neo4jClient;
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
        this.promptService = promptService;
    }

    @Override
    public String performOcr(String filePath, String fileType) {
        // 验证输入参数
        if (!StringUtils.hasText(filePath)) {
            throw new BusinessException("文件路径不能为空");
        }

        // 从数据库获取 OCR Prompt
        String prompt = promptService.getActivePromptContent(BUSINESS_OCR);

        log.debug("开始OCR处理: filePath={}, fileType={}", filePath, fileType);
        
        FileInfo pageFileInfo = null;
        try {
            pageFileInfo = fileStorageService.getFileInfoByUrl(filePath);
            log.debug("成功获取文件信息: {}", pageFileInfo != null ? "有效" : "无效");
        } catch (Exception ex) {
            log.debug("无法通过URL获取文件信息: {}", filePath, ex);
        }

        try {
            byte[] data;
            
            if (pageFileInfo != null) {
                // 使用FileInfo对象下载
                log.debug("使用FileInfo下载文件");
                data = fileStorageService.download(pageFileInfo).bytes();
            } else {
                // 验证文件路径格式
                if (!isValidFileUrl(filePath)) {
                    throw new BusinessException("无效的文件路径格式: " + filePath + 
                        "。请确保路径包含有效的协议前缀（如 http://, https://, file:// 等）");
                }
                
                // 直接使用路径下载
                log.debug("使用文件路径下载文件: {}", filePath);
                data = fileStorageService.download(filePath).bytes();
            }
            
            if (data == null || data.length == 0) {
                throw new BusinessException("文件内容为空: " + filePath);
            }
            
            log.debug("文件下载成功，大小: {} bytes", data.length);

            // 创建ByteArrayResource用于多模态API
            MimeType mimeType = resolveMimeType(fileType);
            String fileName = resolveFileName(filePath);
            String base64Data = Base64.getEncoder().encodeToString(data);
            Image image = Image.builder()
                    .base64Data(base64Data)
                    .mimeType(mimeType.toString())
                    .build();

            UserMessage userMessage = UserMessage.from(List.of(
                    TextContent.from(prompt),
                    ImageContent.from(image)
            ));

            log.debug("准备发送文件到AI模型，mimeType: {}, filename: {}", mimeType, fileName);

            String content = executeChatOperation(BUSINESS_OCR, (chatModel, runtimeConfig) -> {
                Response<AiMessage> response = chatModel.generate(userMessage);
                AiMessage aiMessage = response != null ? response.content() : null;
                return aiMessage != null ? aiMessage.text() : null;
            });
            content = cleanOcrContent(content);
            log.debug("OCR completed: filePath={} characters={} ", filePath, content != null ? content.length() : 0);
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件下载失败: filePath={}, error={}", filePath, e.getMessage(), e);
            throw new BusinessException("文件下载失败: " + e.getMessage() + "。请检查文件路径格式是否正确");
        } finally {
            //deleteOcrSourceFile(pageFileInfo, filePath, fileType);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void performVectorization(String fileId, String content, Integer chunkSize, Integer overlap) {
        log.info("开始向量化: fileId={}", fileId);
        if (!StringUtils.hasText(content)) {
            log.warn("向量化内容为空，跳过: fileId={}", fileId);
            return;
        }

        int effectiveChunkSize = resolveChunkSize(chunkSize);
        int effectiveOverlap = resolveOverlap(overlap, effectiveChunkSize);

        List<Document> documents = splitIntoChunks(content, fileId, effectiveChunkSize, effectiveOverlap);
        if (documents.isEmpty()) {
            log.warn("未生成有效文档块: fileId={}", fileId);
            return;
        }

        List<TextSegment> segments = documents.stream()
                .map(Document::toTextSegment)
                .toList();

        Response<List<Embedding>> response = embeddingModel.embedAll(segments);
        List<Embedding> embeddings = response != null ? response.content() : null;
        if (embeddings == null || embeddings.size() != segments.size()) {
            log.warn("向量化结果数量不匹配: fileId={}, segments={}, embeddings={}",
                    fileId,
                    segments.size(),
                    embeddings != null ? embeddings.size() : 0);
            return;
        }

        embeddingStore.addAll(embeddings, segments);

        log.info("向量化完成: fileId={}, chunks={}", fileId, documents.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateQaPairs(String fileId, String content, Integer maxPairs) {
        log.info("开始生成问答对: fileId={}", fileId);
        if (!StringUtils.hasText(content)) {
            log.warn("问答对生成内容为空，跳过: fileId={}", fileId);
            return;
        }

        FileDetail fileDetail = loadFile(fileId);
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileDetail.getAttr());
        String fileName = StringUtils.hasText(fileDetail.getOriginalFilename()) 
                ? fileDetail.getOriginalFilename() 
                : fileDetail.getFilename();
        
        log.info("========================================");
        log.info("开始问答对生成任务");
        log.info("文件名称: {}", fileName);
        log.info("文件ID: {}", fileId);
        log.info("文件类型: {}", attributes.getFileType());
        log.info("内容长度: {} 字符", content.length());
        log.info("========================================");
        
        // 删除旧的问答对
        qaPairMapper.delete(new LambdaQueryWrapper<KbQaPair>()
                .eq(KbQaPair::getFileId, fileDetail.getId()));

        // 基于Markdown结构智能分段
        List<MarkdownSegment> segments = splitMarkdownIntoSegments(content);
        if (segments.isEmpty()) {
            log.warn("文本分段为空: fileId={}", fileId);
            return;
        }

        int totalSegments = segments.size();
        int totalChars = segments.stream().mapToInt(s -> s.content().length()).sum();
        log.info("Markdown分段完成: 共 {} 个语义段落，总字符数 {}", totalSegments, totalChars);
        log.info("段落长度范围: 最小 {} 字，理想 {} 字，最大 {} 字", 
                MIN_SEGMENT_LENGTH, IDEAL_SEGMENT_LENGTH, MAX_SEGMENT_LENGTH);

        List<QaPairData> allQaPairs = new ArrayList<>();
        int processedSegments = 0;
        
        // 对每个段落生成问答对
        for (int i = 0; i < segments.size(); i++) {
            MarkdownSegment mdSegment = segments.get(i);
            String segment = mdSegment.content();
            
            if (!StringUtils.hasText(segment) || segment.length() < MIN_SEGMENT_LENGTH) {
                log.debug("跳过段落 {}/{}: 内容过短 ({} 字符)", i + 1, totalSegments, segment.length());
                continue;
            }

            try {
                processedSegments++;
                
                // 根据段落长度动态决定生成问题数
                int questionsToGenerate = calculateQuestionCount(segment.length());
                
                String segmentPreview = segment.length() > 60 
                        ? segment.substring(0, 60) + "..." 
                        : segment;
                
                log.info("----------------------------------------");
                log.info("处理进度: [{}/{}] ({}%)", 
                        processedSegments, 
                        totalSegments, 
                        String.format("%.1f", (processedSegments * 100.0 / totalSegments)));
                log.info("段落 #{}: {} | 长度 {} 字符", i + 1, mdSegment.type(), segment.length());
                log.info("段落预览: {}", segmentPreview);
                log.info("生成问题数: {} 个", questionsToGenerate);
                
                // 从数据库获取 QA Prompt 模板并格式化
                String promptTemplate = promptService.getActivePromptContent(BUSINESS_QA);
                Map<String, Object> variables = new HashMap<>();
                variables.put("questionCount", questionsToGenerate);
                variables.put("content", segment);
                String prompt = promptService.formatPrompt(promptTemplate, variables);

                String response = executeChatOperation(BUSINESS_QA, (chatModel, runtimeConfig) ->
                        chatModel.generate(prompt));

                List<QaPairData> segmentQaPairs = parseQaPairs(response, fileId);
                
                // 为每个问答对设置 sourceText
                for (QaPairData qaPair : segmentQaPairs) {
                    allQaPairs.add(new QaPairData(
                            qaPair.question(),
                            qaPair.answer(),
                            segment,  // 使用当前段落作为 sourceText
                            qaPair.confidenceScore()
                    ));
                }
                
                log.info("✓ 段落 #{} 完成: 生成了 {} 个问答对，累计 {} 个", 
                        i + 1, segmentQaPairs.size(), allQaPairs.size());
                
            } catch (Exception e) {
                log.error("✗ 段落 #{} 失败: {}", i + 1, e.getMessage());
                log.warn("失败的段落内容: {}", segment.substring(0, Math.min(50, segment.length())));
            }
        }

        if (allQaPairs.isEmpty()) {
            log.warn("========================================");
            log.warn("问答对生成任务结束");
            log.warn("文件名称: {}", fileName);
            log.warn("警告: 未生成任何问答对");
            log.warn("处理段落数: {}/{}", processedSegments, totalSegments);
            log.warn("========================================");
            return;
        }

        log.info("========================================");
        log.info("开始保存问答对到数据库...");
        
        // 如果指定了最大数量，则截取
        int targetPairs = Optional.ofNullable(maxPairs)
                .filter(value -> value > 0)
                .orElse(DEFAULT_QA_MAX);
        
        List<QaPairData> finalQaPairs = allQaPairs.size() > targetPairs 
                ? allQaPairs.subList(0, targetPairs) 
                : allQaPairs;
        
        if (allQaPairs.size() > targetPairs) {
            log.info("问答对数量超过限制，从 {} 个截取到 {} 个", allQaPairs.size(), targetPairs);
        }

        // 保存到数据库
        int savedCount = 0;
        for (QaPairData qaPair : finalQaPairs) {
            KbQaPair entity = KbQaPair.builder()
                    .qaId(IdGenerator.simpleUUID())
                    .fileId(fileDetail.getId())
                    .question(qaPair.question())
                    .answer(qaPair.answer())
                    .sourceText(qaPair.sourceText())
                    .confidenceScore(Optional.ofNullable(qaPair.confidenceScore()).orElse(DEFAULT_QA_CONFIDENCE))
                    .build();
            qaPairMapper.insert(entity);
            savedCount++;
        }

        log.info("========================================");
        log.info("问答对生成任务完成！");
        log.info("文件名称: {}", fileName);
        log.info("文件ID: {}", fileId);
        log.info("处理段落数: {}/{}", processedSegments, totalSegments);
        log.info("生成问答对: {} 个", allQaPairs.size());
        log.info("保存问答对: {} 个", savedCount);
        log.info("平均每段生成: {}", 
                processedSegments > 0 ? String.format("%.1f", (allQaPairs.size() * 1.0 / processedSegments)) : "0");
        log.info("========================================");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateKnowledgeGraph(String fileId, String content) {
        log.info("开始生成知识图谱: fileId={}", fileId);
        FileDetail fileDetail = loadFile(fileId);
        FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileDetail.getAttr());

        List<KbDocument> documentPages = loadCompletedPages(fileId);
        boolean hasPageContent = !documentPages.isEmpty();
        if (!hasPageContent && !StringUtils.hasText(content)) {
            log.warn("知识图谱生成内容为空，跳过: fileId={}", fileId);
            return;
        }

        List<ChunkExtractionResult> chunkResults = new ArrayList<>();

        if (hasPageContent) {
            int totalPages = documentPages.size();
            List<List<KbDocument>> pageChunks = partitionPages(documentPages, KG_PAGES_PER_REQUEST);
            int totalChunks = pageChunks.size();
            for (int index = 0; index < totalChunks; index++) {
                List<KbDocument> chunk = pageChunks.get(index);
                String chunkContent = buildChunkContent(chunk);
                if (!StringUtils.hasText(chunkContent)) {
                    log.debug("跳过空白页面块: fileId={} chunkIndex={}", fileId, index + 1);
                    continue;
                }

                String prompt = buildKnowledgeGraphPrompt(fileDetail, attributes, index + 1, totalChunks, totalPages, chunkContent, chunk);
                String response = executeChatOperation(BUSINESS_KG, (chatModel, runtimeConfig) ->
                        chatModel.generate(prompt));

                KnowledgeGraphData chunkData = parseKnowledgeGraph(response, fileId);
                if (chunkData.entities().isEmpty() && chunkData.relationships().isEmpty()) {
                    log.debug("知识图谱块提取为空: fileId={} chunkIndex={}", fileId, index + 1);
                    continue;
                }

                Set<Integer> pageNumbers = chunk.stream()
                        .map(KbDocument::getPageIndex)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                chunkResults.add(new ChunkExtractionResult(pageNumbers, chunkData));
            }
        } else {
            String normalizedContent = StringUtils.trimWhitespace(content);
            if (!StringUtils.hasText(normalizedContent)) {
                log.warn("知识图谱生成内容为空，跳过: fileId={}", fileId);
                return;
            }

            String prompt = buildKnowledgeGraphPrompt(fileDetail, attributes, 1, 1, 1, normalizedContent, Collections.emptyList());
            String response = executeChatOperation(BUSINESS_KG, (chatModel, runtimeConfig) ->
                    chatModel.generate(prompt));

            KnowledgeGraphData chunkData = parseKnowledgeGraph(response, fileId);
            if (!chunkData.entities().isEmpty() || !chunkData.relationships().isEmpty()) {
                chunkResults.add(new ChunkExtractionResult(Collections.emptySet(), chunkData));
            }
        }

        KnowledgeGraphData mergedGraph = mergeChunkResults(chunkResults);
        persistKnowledgeGraph(fileDetail, mergedGraph);
        log.info("知识图谱生成完成: fileId={} entities={} relationships={}",
                fileId,
                mergedGraph.entities().size(),
                mergedGraph.relationships().size());
    }

    @Override
    public List<String> generateTags(String fileId, String fileName, String content) {
        log.info("========================================");
        log.info("开始生成文件标签");
        log.info("文件ID: {}", fileId);
        log.info("文件名称: {}", fileName);
        log.info("内容长度: {} 字符", content != null ? content.length() : 0);
        log.info("========================================");

        if (!StringUtils.hasText(content)) {
            log.warn("内容为空，无法生成标签: fileId={}", fileId);
            return List.of();
        }

        // 截取前500字作为预览
        String contentPreview = content.length() > TAG_CONTENT_PREVIEW_LENGTH
                ? content.substring(0, TAG_CONTENT_PREVIEW_LENGTH)
                : content;

        log.info("内容预览: {} 字符", contentPreview.length());

        // 从数据库获取 TAG Prompt 模板并格式化
        String promptTemplate = promptService.getActivePromptContent(BUSINESS_TAG);
        Map<String, Object> variables = new HashMap<>();
        variables.put("tagCount", TAG_COUNT);
        variables.put("fileName", fileName);
        variables.put("content", contentPreview);
        String prompt = promptService.formatPrompt(promptTemplate, variables);

        try {
            String response = executeChatOperation(BUSINESS_TAG, (chatModel, runtimeConfig) ->
                    chatModel.generate(prompt));

            List<String> tags = parseTags(response, fileId);

            log.info("========================================");
            log.info("标签生成完成！");
            log.info("文件名称: {}", fileName);
            log.info("文件ID: {}", fileId);
            log.info("生成标签: {}", tags);
            log.info("标签数量: {}", tags.size());
            log.info("========================================");

            return tags;

        } catch (Exception e) {
            log.error("标签生成失败: fileId={}, error={}", fileId, e.getMessage(), e);
            return List.of();
        }
    }

    private List<String> parseTags(String response, String fileId) {
        String json = extractJson(response);
        if (!StringUtils.hasText(json)) {
            log.warn("标签响应为空或格式不正确: fileId={}", fileId);
            return List.of();
        }

        try {
            List<String> tags = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });

            // 过滤空标签和过长标签
            return tags.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .filter(tag -> tag.length() >= 2 && tag.length() <= 20)
                    .limit(TAG_COUNT)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("解析标签结果失败: fileId={} response={}", fileId, response, e);
            return List.of();
        }
    }

    private List<KbDocument> loadCompletedPages(String fileId) {
        return kbDocumentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                .eq(KbDocument::getFileId, fileId)
                .eq(KbDocument::getOcrStatus, ProcessingStatus.COMPLETED.getCode())
                .orderByAsc(KbDocument::getPageIndex));
    }

    private List<List<KbDocument>> partitionPages(List<KbDocument> pages, int pageSize) {
        if (pages == null || pages.isEmpty()) {
            return List.of();
        }
        List<List<KbDocument>> chunks = new ArrayList<>();
        for (int index = 0; index < pages.size(); index += Math.max(pageSize, 1)) {
            int end = Math.min(index + Math.max(pageSize, 1), pages.size());
            chunks.add(pages.subList(index, end));
        }
        return chunks;
    }

    private String buildChunkContent(List<KbDocument> chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (KbDocument page : chunk) {
            builder.append("【第").append(page.getPageIndex()).append("页】").append('\n');
            String pageContent = StringUtils.hasText(page.getContent())
                    ? page.getContent().trim()
                    : "(空白页，无有效文本)";
            builder.append(pageContent).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String buildKnowledgeGraphPrompt(FileDetail fileDetail,
                                             FileDetailAttributes attributes,
                                             int chunkIndex,
                                             int totalChunks,
                                             int totalPages,
                                             String chunkContent,
                                             List<KbDocument> chunkPages) {
        String documentName = StringUtils.hasText(fileDetail.getOriginalFilename())
                ? fileDetail.getOriginalFilename()
                : fileDetail.getFilename();
        String fileType = Optional.ofNullable(attributes.getFileType()).orElse("unknown");
        String pageSummary = chunkPages == null || chunkPages.isEmpty()
                ? "全文"
                : chunkPages.stream()
                .map(KbDocument::getPageIndex)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        // 从数据库获取 KG Prompt 模板并格式化
        String promptTemplate = promptService.getActivePromptContent(BUSINESS_KG);
        Map<String, Object> variables = new HashMap<>();
        variables.put("documentName", documentName);
        variables.put("fileType", fileType);
        variables.put("totalPages", Math.max(totalPages, 1));
        variables.put("chunkIndex", Math.max(chunkIndex, 1));
        variables.put("totalChunks", Math.max(totalChunks, 1));
        variables.put("pageSummary", pageSummary);
        variables.put("content", chunkContent);
        
        return promptService.formatPrompt(promptTemplate, variables);
    }

    private KnowledgeGraphData mergeChunkResults(List<ChunkExtractionResult> chunkResults) {
        if (chunkResults == null || chunkResults.isEmpty()) {
            return KnowledgeGraphData.empty();
        }

        Map<String, AggregatedEntity> entityMap = new LinkedHashMap<>();
        Map<String, AggregatedEntity> lookupIndex = new HashMap<>();

        for (ChunkExtractionResult chunkResult : chunkResults) {
            Set<Integer> pages = chunkResult.pageNumbers();
            for (GraphEntity entity : chunkResult.data().entities()) {
                if (!StringUtils.hasText(entity.name())) {
                    continue;
                }
                AggregatedEntity aggregated = resolveAggregatedEntity(entityMap, lookupIndex, entity);
                aggregated.merge(entity, pages);
                registerAliasMappings(lookupIndex, aggregated);
            }
        }

        Map<String, AggregatedRelationship> relationshipMap = new LinkedHashMap<>();
        for (ChunkExtractionResult chunkResult : chunkResults) {
            Set<Integer> pages = chunkResult.pageNumbers();
            for (GraphRelationship relationship : chunkResult.data().relationships()) {
                AggregatedEntity fromEntity = findEntityByReference(relationship.fromId(), relationship.fromName(), entityMap, lookupIndex);
                AggregatedEntity toEntity = findEntityByReference(relationship.toId(), relationship.toName(), entityMap, lookupIndex);
                if (fromEntity == null || toEntity == null) {
                    continue;
                }

                String relationshipType = sanitizeRelationshipType(relationship.type());
                String key = fromEntity.getPreferredId() + "->" + toEntity.getPreferredId() + "::" + relationshipType;
                AggregatedRelationship aggregatedRelationship = relationshipMap.computeIfAbsent(key,
                        k -> new AggregatedRelationship(
                                fromEntity.getPreferredId(),
                                toEntity.getPreferredId(),
                                fromEntity.getDisplayName(),
                                toEntity.getDisplayName(),
                                relationshipType));
                aggregatedRelationship.merge(relationship, pages);
            }
        }

        List<GraphEntity> entities = entityMap.values().stream()
                .map(AggregatedEntity::toGraphEntity)
                .toList();

        List<GraphRelationship> relationships = relationshipMap.values().stream()
                .map(AggregatedRelationship::toGraphRelationship)
                .toList();

        return new KnowledgeGraphData(entities, relationships);
    }

    private AggregatedEntity resolveAggregatedEntity(Map<String, AggregatedEntity> entityMap,
                                                     Map<String, AggregatedEntity> lookupIndex,
                                                     GraphEntity entity) {
        String normalizedId = normalizeEntityIdentifier(entity.id());
        if (StringUtils.hasText(normalizedId)) {
            AggregatedEntity existing = lookupIndex.get(normalizedId);
            if (existing != null) {
                return existing;
            }
        }

        String normalizedName = normalizeEntityName(entity.name());
        if (StringUtils.hasText(normalizedName)) {
            AggregatedEntity existing = lookupIndex.get(normalizedName);
            if (existing != null) {
                return existing;
            }
        }

        if (entity.aliases() != null) {
            for (String alias : entity.aliases()) {
                String aliasNormalized = normalizeEntityName(alias);
                if (StringUtils.hasText(aliasNormalized)) {
                    AggregatedEntity existing = lookupIndex.get(aliasNormalized);
                    if (existing != null) {
                        return existing;
                    }
                }
            }
        }

        String canonicalKey;
        if (StringUtils.hasText(normalizedId)) {
            canonicalKey = normalizedId;
        } else if (StringUtils.hasText(normalizedName)) {
            canonicalKey = normalizedName;
        } else {
            canonicalKey = IdGenerator.simpleUUID();
        }

        return entityMap.computeIfAbsent(canonicalKey, key -> new AggregatedEntity(key, entity));
    }

    private void registerAliasMappings(Map<String, AggregatedEntity> lookupIndex, AggregatedEntity aggregated) {
        for (String alias : aggregated.getAllNames()) {
            String normalized = normalizeEntityName(alias);
            if (StringUtils.hasText(normalized)) {
                lookupIndex.put(normalized, aggregated);
            }
        }
        for (String identifier : aggregated.getAllIdentifiers()) {
            String normalized = normalizeEntityIdentifier(identifier);
            if (StringUtils.hasText(normalized)) {
                lookupIndex.put(normalized, aggregated);
            }
        }
    }

    private AggregatedEntity findEntityByReference(String id,
                                                   String name,
                                                   Map<String, AggregatedEntity> entityMap,
                                                   Map<String, AggregatedEntity> lookupIndex) {
        if (StringUtils.hasText(id)) {
            String normalizedId = normalizeEntityIdentifier(id);
            if (StringUtils.hasText(normalizedId)) {
                AggregatedEntity existing = lookupIndex.get(normalizedId);
                if (existing != null) {
                    return existing;
                }
            }
        }

        if (StringUtils.hasText(name)) {
            String normalizedName = normalizeEntityName(name);
            if (StringUtils.hasText(normalizedName)) {
                AggregatedEntity existing = lookupIndex.get(normalizedName);
                if (existing != null) {
                    return existing;
                }
            }
        }

        if (StringUtils.hasText(name)) {
            for (AggregatedEntity candidate : entityMap.values()) {
                if (candidate.matches(name)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private String normalizeEntityName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String trimmed = Normalizer.normalize(name.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("\\s+", " ");
    }

    private boolean areNamesSimilar(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }
        String compactLeft = left.replaceAll("\\s+", "");
        String compactRight = right.replaceAll("\\s+", "");
        if (compactLeft.equals(compactRight)) {
            return true;
        }
        if (compactLeft.contains(compactRight) || compactRight.contains(compactLeft)) {
            return true;
        }
        int distance = levenshteinDistance(compactLeft, compactRight);
        return distance >= 0 && distance <= 2;
    }

    private int levenshteinDistance(String left, String right) {
        if (left == null || right == null) {
            return -1;
        }
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[left.length()][right.length()];
    }

    private int resolveChunkSize(Integer chunkSize) {
        if (chunkSize == null || chunkSize < MIN_CHUNK_SIZE) {
            return DEFAULT_CHUNK_SIZE;
        }
        return chunkSize;
    }

    private int resolveOverlap(Integer overlap, int chunkSize) {
        int effective = Optional.ofNullable(overlap).filter(value -> value >= 0).orElse(DEFAULT_OVERLAP);
        return Math.min(effective, chunkSize - 1);
    }

    private MimeType resolveMimeType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }

        String normalized = fileType.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pdf" -> MimeTypeUtils.parseMimeType("application/pdf");
            case "word", "doc", "docx" -> MimeTypeUtils.parseMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "image", "jpg", "jpeg" -> MimeTypeUtils.IMAGE_JPEG;
            case "png" -> MimeTypeUtils.IMAGE_PNG;
            case "txt", "text" -> MimeTypeUtils.TEXT_PLAIN;
            default -> MimeTypeUtils.APPLICATION_OCTET_STREAM;
        };
    }

    private void deleteOcrSourceFile(FileInfo fileInfo, String filePath, String fileType) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }
        if (!StringUtils.hasText(fileType) || !"image".equalsIgnoreCase(fileType.trim())) {
            return;
        }
        String trimmedPath = filePath.trim();
        if (trimmedPath.isEmpty()) {
            return;
        }
        try {
            boolean deleted = fileInfo != null
                    ? fileStorageService.delete(fileInfo)
                    : fileStorageService.delete(trimmedPath);
            if (!deleted) {
                log.warn("Failed to delete OCR source image after processing: {}", trimmedPath);
            }
        } catch (Exception ex) {
            log.warn("Error deleting OCR source image after processing: {}", trimmedPath, ex);
        }
    }

    private String cleanOcrContent(String content) {
        if (content == null) {
            return null;
        }
        String sanitized = StringUtils.trimWhitespace(content);
        sanitized = removeOcrAssistantPrefix(sanitized);
        sanitized = removeMarkdownCodeFenceWrapper(sanitized);
        return StringUtils.trimWhitespace(sanitized);
    }

    private String removeOcrAssistantPrefix(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        List<String> prefixes = List.of(
                "作为一个专业的OCR识别助手，我已识别图片中的所有文字内容，并严格按照您的要求进行格式化。",
                "好的，这是对图片内容的OCR识别结果，已按照您的要求格式化为Markdown。"
        );
        String sanitized = content;
        for (String prefix : prefixes) {
            if (sanitized.startsWith(prefix)) {
                sanitized = sanitized.substring(prefix.length());
                sanitized = StringUtils.trimLeadingWhitespace(sanitized);
                break;
            }
        }
        return sanitized;
    }

    private String removeMarkdownCodeFenceWrapper(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        String sanitized = StringUtils.trimWhitespace(content);
        if (!sanitized.startsWith("```")) {
            return sanitized;
        }
        int newlineIndex = sanitized.indexOf('\n');
        if (newlineIndex < 0) {
            return sanitized;
        }
        String fenceDescriptor = sanitized.substring(3, newlineIndex).trim();
        if (!fenceDescriptor.isEmpty() && !"markdown".equalsIgnoreCase(fenceDescriptor) && !"md".equalsIgnoreCase(fenceDescriptor)) {
            return sanitized;
        }
        String withoutOpeningFence = sanitized.substring(newlineIndex + 1);
        String trimmedWithoutOpening = StringUtils.trimWhitespace(withoutOpeningFence);
        if (trimmedWithoutOpening.endsWith("```")) {
            trimmedWithoutOpening = trimmedWithoutOpening.substring(0, trimmedWithoutOpening.length() - 3);
        }
        return StringUtils.trimWhitespace(trimmedWithoutOpening);
    }

    /**
     * 验证文件URL是否有效
     * 
     * @param filePath 文件路径
     * @return 是否为有效的URL格式
     */
    private boolean isValidFileUrl(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return false;
        }
        
        String trimmedPath = filePath.trim();
        
        // 检查是否包含协议前缀
        if (!trimmedPath.contains("://")) {
            log.debug("文件路径缺少协议前缀: {}", trimmedPath);
            return false;
        }
        
        try {
            URI uri = new URI(trimmedPath);
            String scheme = uri.getScheme();
            
            // 验证协议是否有效
            if (!StringUtils.hasText(scheme)) {
                log.debug("文件路径缺少有效协议: {}", trimmedPath);
                return false;
            }
            
            // 支持的协议
            String lowerScheme = scheme.toLowerCase(Locale.ROOT);
            boolean validScheme = lowerScheme.equals("http") || 
                                lowerScheme.equals("https") || 
                                lowerScheme.equals("file") ||
                                lowerScheme.equals("ftp") ||
                                lowerScheme.equals("ftps");
            
            if (!validScheme) {
                log.debug("不支持的协议类型: {}", scheme);
                return false;
            }
            
            log.debug("文件路径验证通过: scheme={}, path={}", scheme, trimmedPath);
            return true;
            
        } catch (URISyntaxException e) {
            log.debug("无效的URI格式: {}, error={}", trimmedPath, e.getMessage());
            return false;
        }
    }

    private String resolveFileName(String path) {
        if (!StringUtils.hasText(path)) {
            return "file";
        }
        String normalized = path.replace('\\', '/');
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < normalized.length() - 1) {
            return normalized.substring(lastSlash + 1);
        }
        return normalized;
    }

    private List<Document> splitIntoChunks(String content, String fileId, int chunkSize, int overlap) {
        List<Document> documents = new ArrayList<>();
        if (!StringUtils.hasText(content)) {
            return documents;
        }

        int start = 0;
        int chunkIndex = 0;
        String normalized = content.trim();
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            String chunk = normalized.substring(start, end);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileId", fileId);
            metadata.put("chunkIndex", chunkIndex);
            documents.add(new Document(chunk, Metadata.from(metadata)));
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(0, end - overlap);
            chunkIndex++;
        }
        return documents;
    }

    /**
     * 根据段落长度计算应该生成的问题数量
     * 
     * @param segmentLength 段落长度（字符数）
     * @return 问题数量
     */
    private int calculateQuestionCount(int segmentLength) {
        if (segmentLength < 200) {
            return 3;   // 简短段落
        } else if (segmentLength < 400) {
            return 5;   // 标准段落
        } else if (segmentLength < 600) {
            return 7;   // 较长段落
        } else if (segmentLength < 1000) {
            return 10;  // 长段落
        } else {
            return 12;  // 超长段落
        }
    }

    /**
     * 基于Markdown结构智能分段
     * 识别标题、代码块、表格、列表等语义单元
     * 
     * @param content Markdown内容
     * @return 分段列表
     */
    private List<MarkdownSegment> splitMarkdownIntoSegments(String content) {
        List<MarkdownSegment> segments = new ArrayList<>();
        if (!StringUtils.hasText(content)) {
            return segments;
        }

        String[] lines = content.split("\n");
        StringBuilder currentSegment = new StringBuilder();
        String currentType = "paragraph";
        boolean inCodeBlock = false;
        boolean inTable = false;
        List<String> tempBuffer = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            // 代码块处理
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    // 代码块结束
                    currentSegment.append(line).append("\n");
                    addSegmentIfValid(segments, currentSegment.toString(), "代码块");
                    currentSegment = new StringBuilder();
                    inCodeBlock = false;
                } else {
                    // 保存之前的内容
                    if (currentSegment.length() > 0) {
                        addSegmentIfValid(segments, currentSegment.toString(), currentType);
                        currentSegment = new StringBuilder();
                    }
                    // 代码块开始
                    currentSegment.append(line).append("\n");
                    inCodeBlock = true;
                    currentType = "代码块";
                }
                continue;
            }

            // 在代码块内
            if (inCodeBlock) {
                currentSegment.append(line).append("\n");
                continue;
            }

            // 标题检测
            if (trimmedLine.matches("^#{1,6}\\s+.+")) {
                // 保存之前的段落
                if (currentSegment.length() > 0) {
                    addSegmentIfValid(segments, currentSegment.toString(), currentType);
                    currentSegment = new StringBuilder();
                }
                
                // 提取标题级别
                int level = 0;
                for (char c : trimmedLine.toCharArray()) {
                    if (c == '#') level++;
                    else break;
                }
                currentType = level <= 2 ? "标题段落" : "子标题段落";
                currentSegment.append(line).append("\n");
                
                // 继续收集该标题下的内容
                continue;
            }

            // 表格检测（简单判断：以|开头）
            if (trimmedLine.startsWith("|") && trimmedLine.endsWith("|")) {
                if (!inTable) {
                    // 保存之前的内容
                    if (currentSegment.length() > 0) {
                        addSegmentIfValid(segments, currentSegment.toString(), currentType);
                        currentSegment = new StringBuilder();
                    }
                    inTable = true;
                    currentType = "表格";
                }
                currentSegment.append(line).append("\n");
                continue;
            } else if (inTable) {
                // 表格结束
                addSegmentIfValid(segments, currentSegment.toString(), currentType);
                currentSegment = new StringBuilder();
                inTable = false;
                currentType = "paragraph";
            }

            // 列表检测
            if (trimmedLine.matches("^[-*+]\\s+.+") || trimmedLine.matches("^\\d+\\.\\s+.+")) {
                if (!currentType.equals("列表")) {
                    if (currentSegment.length() > 0) {
                        addSegmentIfValid(segments, currentSegment.toString(), currentType);
                        currentSegment = new StringBuilder();
                    }
                    currentType = "列表";
                }
                currentSegment.append(line).append("\n");
                continue;
            } else if (currentType.equals("列表") && !StringUtils.hasText(trimmedLine)) {
                // 列表结束（空行）
                addSegmentIfValid(segments, currentSegment.toString(), currentType);
                currentSegment = new StringBuilder();
                currentType = "paragraph";
                continue;
            }

            // 空行处理（段落分隔）
            if (!StringUtils.hasText(trimmedLine)) {
                if (currentSegment.length() > 0 && !currentType.equals("列表")) {
                    addSegmentIfValid(segments, currentSegment.toString(), currentType);
                    currentSegment = new StringBuilder();
                    currentType = "paragraph";
                }
                continue;
            }

            // 普通文本
            currentSegment.append(line).append("\n");
            
            // 如果当前段落过长，尝试分割
            if (currentSegment.length() > MAX_SEGMENT_LENGTH && !inCodeBlock && !inTable) {
                addSegmentIfValid(segments, currentSegment.toString(), currentType);
                currentSegment = new StringBuilder();
            }
        }

        // 处理剩余内容
        if (currentSegment.length() > 0) {
            addSegmentIfValid(segments, currentSegment.toString(), currentType);
        }

        // 合并过短的段落
        return mergeShortSegments(segments);
    }

    /**
     * 添加段落到列表（如果满足最小长度要求）
     */
    private void addSegmentIfValid(List<MarkdownSegment> segments, String content, String type) {
        String trimmed = content.trim();
        if (trimmed.length() >= MIN_SEGMENT_LENGTH) {
            segments.add(new MarkdownSegment(type, trimmed));
        } else if (trimmed.length() > 20) {
            // 稍短的段落也保留，后续会尝试合并
            segments.add(new MarkdownSegment(type, trimmed));
        }
    }

    /**
     * 合并过短的段落
     */
    private List<MarkdownSegment> mergeShortSegments(List<MarkdownSegment> segments) {
        if (segments.isEmpty()) {
            return segments;
        }

        List<MarkdownSegment> merged = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String bufferType = segments.get(0).type();

        for (MarkdownSegment segment : segments) {
            // 代码块和表格不合并
            if (segment.type().equals("代码块") || segment.type().equals("表格")) {
                if (buffer.length() > 0) {
                    merged.add(new MarkdownSegment(bufferType, buffer.toString().trim()));
                    buffer = new StringBuilder();
                }
                merged.add(segment);
                bufferType = "paragraph";
                continue;
            }

            // 如果当前buffer为空，开始新的buffer
            if (buffer.length() == 0) {
                buffer.append(segment.content());
                bufferType = segment.type();
            }
            // 如果加上当前段落仍然小于理想长度，合并
            else if (buffer.length() + segment.content().length() < IDEAL_SEGMENT_LENGTH) {
                buffer.append("\n\n").append(segment.content());
            }
            // 否则保存当前buffer，开始新的
            else {
                merged.add(new MarkdownSegment(bufferType, buffer.toString().trim()));
                buffer = new StringBuilder(segment.content());
                bufferType = segment.type();
            }
        }

        // 添加最后的buffer
        if (buffer.length() > 0) {
            merged.add(new MarkdownSegment(bufferType, buffer.toString().trim()));
        }

        return merged;
    }

    /**
     * Markdown段落数据结构
     */
    private record MarkdownSegment(String type, String content) {
    }

    private List<QaPairData> parseQaPairs(String response, String fileId) {
        String json = extractJson(response);
        if (!StringUtils.hasText(json)) {
            log.warn("问答对响应为空或格式不正确: fileId={}", fileId);
            return List.of();
        }

        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
            List<QaPairData> results = new ArrayList<>();
            for (Map<String, Object> item : rawList) {
                String question = toString(item.get("question"));
                String answer = toString(item.get("answer"));
                if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
                    continue;
                }
                String sourceText = Optional.ofNullable(toString(item.get("sourceText")))
                        .orElseGet(() -> toString(item.get("source_text")));
                BigDecimal confidence = toBigDecimal(Optional.ofNullable(item.get("confidenceScore"))
                        .orElse(item.get("confidence_score")));
                results.add(new QaPairData(question, answer, sourceText, confidence));
            }
            return results;
        } catch (Exception e) {
            log.error("解析问答对结果失败: fileId={} response={}", fileId, response, e);
            return List.of();
        }
    }

    private <T> T executeChatOperation(String businessCode, ChatOperation<T> operation) {
        LinkedHashSet<String> attemptedKeys = new LinkedHashSet<>();
        RuntimeException lastError = null;

        for (int attempt = 0; attempt < MAX_KEY_ATTEMPTS; attempt++) {
            ModelRuntimeConfig runtimeConfig;
            try {
                runtimeConfig = modelConfigService.getRuntimeConfig(businessCode, attemptedKeys);
            } catch (BusinessException ex) {
                if (lastError != null) {
                    throw lastError;
                }
                throw ex;
            }

            AIModelFactory.ChatModelContext context = aiModelFactory.createChatModelContext(runtimeConfig);
            String keyId = runtimeConfig.apiKey() != null ? runtimeConfig.apiKey().getKeyId() : null;

            try {
                T result = operation.execute(context.chatModel(), runtimeConfig);
                recordApiKeyUsage(runtimeConfig, true, null);
                return result;
            } catch (RuntimeException ex) {
                recordApiKeyUsage(runtimeConfig, false, ex.getMessage());
                if (keyId != null) {
                    attemptedKeys.add(keyId);
                }
                lastError = ex;
                log.warn("Chat operation failed for provider {} key {}: {}", runtimeConfig.provider(),
                        runtimeConfig.apiKey() != null ? runtimeConfig.apiKey().getDisplayKey() : "n/a",
                        ex.getMessage());
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new BusinessException("No available API key for business: " + businessCode);
    }

    private void recordApiKeyUsage(ModelRuntimeConfig runtimeConfig, boolean success, String error) {
        if (runtimeConfig.apiKey() == null || !StringUtils.hasText(runtimeConfig.apiKey().getKeyId())) {
            return;
        }
        modelConfigService.recordApiKeyUsage(runtimeConfig.apiKey().getKeyId(), success, error);
    }

    @FunctionalInterface
    private interface ChatOperation<T> {
        T execute(ChatLanguageModel chatModel, ModelRuntimeConfig runtimeConfig);
    }

    private KnowledgeGraphData parseKnowledgeGraph(String response, String fileId) {
        String json = extractJson(response);
        if (!StringUtils.hasText(json)) {
            log.warn("知识图谱响应为空或格式不正确: fileId={}", fileId);
            return KnowledgeGraphData.empty();
        }

        log.info("知识图谱模型输出: fileId={} json={}", fileId, json);

        try {
            JsonNode root = objectMapper.readTree(json);
            List<GraphEntity> entities = new ArrayList<>();
            if (root.has("entities") && root.get("entities").isArray()) {
                for (JsonNode entityNode : root.get("entities")) {
                    String name = toString(entityNode.get("name"));
                    String id = ensureEntityIdentifier(toString(entityNode.get("id")), name);
                    if (!StringUtils.hasText(id)) {
                        continue;
                    }
                    if (!StringUtils.hasText(name)) {
                        name = id;
                    }
                    String type = toString(entityNode.get("type"));
                    String description = toString(entityNode.get("description"));
                    List<String> aliases = readStringArray(entityNode, "aliases");
                    List<Integer> sourcePages = readIntegerArray(entityNode, "sourcePages");
                    entities.add(new GraphEntity(id, name, type, description, aliases, sourcePages));
                }
            }

            List<GraphRelationship> relationships = new ArrayList<>();
            if (root.has("relationships") && root.get("relationships").isArray()) {
                for (JsonNode relNode : root.get("relationships")) {
                    String fromName = toString(relNode.get("from"));
                    String toName = toString(relNode.get("to"));
                    String fromId = ensureEntityIdentifier(toString(relNode.get("fromId")), fromName);
                    String toId = ensureEntityIdentifier(toString(relNode.get("toId")), toName);
                    if (!StringUtils.hasText(fromId) || !StringUtils.hasText(toId)) {
                        continue;
                    }
                    String type = toString(relNode.get("type"));
                    String description = toString(relNode.get("description"));
                    List<Integer> sourcePages = readIntegerArray(relNode, "sourcePages");
                    relationships.add(new GraphRelationship(fromId, toId, type, description, sourcePages, fromName, toName));
                }
            }
            return new KnowledgeGraphData(entities, relationships);
        } catch (Exception e) {
            log.error("解析知识图谱结果失败: fileId={} response={}", fileId, response, e);
            return KnowledgeGraphData.empty();
        }
    }

    private List<String> readStringArray(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || !node.get(fieldName).isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node.get(fieldName)) {
            if (item != null && item.isTextual()) {
                String value = item.asText();
                if (StringUtils.hasText(value)) {
                    values.add(value.trim());
                }
            }
        }
        return values;
    }

    private List<Integer> readIntegerArray(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || !node.get(fieldName).isArray()) {
            return List.of();
        }
        List<Integer> values = new ArrayList<>();
        for (JsonNode item : node.get(fieldName)) {
            if (item == null) {
                continue;
            }
            if (item.isInt() || item.isLong()) {
                values.add(item.asInt());
            } else if (item.isTextual()) {
                String raw = item.asText();
                if (StringUtils.hasText(raw)) {
                    try {
                        values.add(Integer.parseInt(raw.trim()));
                    } catch (NumberFormatException ignored) {
                        // ignore invalid number
                    }
                }
            }
        }
        return values;
    }

    private String ensureEntityIdentifier(String candidateId, String fallbackName) {
        String sanitized = normalizeEntityIdentifier(candidateId);
        if (StringUtils.hasText(sanitized)) {
            return sanitized;
        }
        String derived = normalizeEntityIdentifier(fallbackName);
        if (StringUtils.hasText(derived)) {
            return derived;
        }
        return "entity-" + IdGenerator.simpleUUID();
    }

    private String normalizeEntityIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() > 80) {
            normalized = normalized.substring(0, 80);
        }
        return normalized;
    }

    private String buildEntityNodeId(String fileId, String entityExternalId) {
        return fileId + "::" + entityExternalId;
    }

    private void registerEntityLookup(Map<String, String> lookup, String value, String nodeId, boolean identifier) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(nodeId)) {
            return;
        }
        String normalized = identifier ? normalizeEntityIdentifier(value) : normalizeEntityName(value);
        if (StringUtils.hasText(normalized)) {
            lookup.put(normalized, nodeId);
        }
    }

    private String resolveEntityNodeId(Map<String, String> lookup, String id, String name) {
        if (lookup.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(id)) {
            String normalizedId = normalizeEntityIdentifier(id);
            if (StringUtils.hasText(normalizedId)) {
                String nodeId = lookup.get(normalizedId);
                if (StringUtils.hasText(nodeId)) {
                    return nodeId;
                }
            }
        }
        if (StringUtils.hasText(name)) {
            String normalizedName = normalizeEntityName(name);
            if (StringUtils.hasText(normalizedName)) {
                String nodeId = lookup.get(normalizedName);
                if (StringUtils.hasText(nodeId)) {
                    return nodeId;
                }
            }
        }
        return null;
    }

    private void persistKnowledgeGraph(FileDetail fileDetail, KnowledgeGraphData graphData) {
        log.info("开始持久化知识图谱: fileId={}, entities={}, relationships={}", 
            fileDetail.getId(), graphData.entities().size(), graphData.relationships().size());
        
        // 删除旧数据
        try {
            documentNodeRepository.deleteDocumentWithRelations(fileDetail.getId());
            log.info("删除旧知识图谱数据成功: fileId={}", fileDetail.getId());
        } catch (Exception e) {
            log.error("删除旧知识图谱数据失败: fileId={}", fileDetail.getId(), e);
            throw new BusinessException("删除旧知识图谱数据失败: " + e.getMessage());
        }

        FileDetailAttributes attributes = FileDetailAttrUtils.parse(fileDetail.getAttr());
        String documentName = StringUtils.hasText(fileDetail.getOriginalFilename())
                ? fileDetail.getOriginalFilename()
                : fileDetail.getFilename();

        // 保存文档节点
        DocumentNode documentNode = DocumentNode.builder()
                .id(fileDetail.getId())
                .name(documentName)
                .type(Optional.ofNullable(attributes.getFileType()).orElse("document"))
                .createdAt(LocalDateTime.now())
                .build();
        documentNodeRepository.save(documentNode);
        log.info("保存文档节点成功: id={}, name={}", documentNode.getId(), documentNode.getName());

        if (graphData.entities().isEmpty()) {
            log.warn("知识图谱没有实体数据: fileId={}", fileDetail.getId());
            return;
        }

        Map<String, String> entityNodeLookup = new HashMap<>();
        int savedEntityCount = 0;
        for (GraphEntity entity : graphData.entities()) {
            String externalId = StringUtils.hasText(entity.id()) ? entity.id() : "entity-" + IdGenerator.simpleUUID();
            String normalizedExternalId = normalizeEntityIdentifier(externalId);
            if (!StringUtils.hasText(normalizedExternalId)) {
                normalizedExternalId = "entity-" + IdGenerator.simpleUUID();
            }
            String nodeId = buildEntityNodeId(fileDetail.getId(), normalizedExternalId);

            // 准备aliases数组
            String[] aliasArray = null;
            if (entity.aliases() != null && !entity.aliases().isEmpty()) {
                aliasArray = new LinkedHashSet<>(entity.aliases())
                        .stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .toArray(String[]::new);
                if (aliasArray.length == 0) {
                    aliasArray = null;
                }
            }

            // 准备sourcePages数组
            int[] pageArray = null;
            if (entity.sourcePages() != null && !entity.sourcePages().isEmpty()) {
                pageArray = entity.sourcePages().stream()
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .distinct()
                        .sorted()
                        .toArray();
                if (pageArray.length == 0) {
                    pageArray = null;
                }
            }

            // 直接设置各个字段而不是使用Map
            EntityNode entityNode = EntityNode.builder()
                    .id(nodeId)
                    .name(entity.name())
                    .type(StringUtils.hasText(entity.type()) ? entity.type() : "concept")
                    .externalId(externalId)
                    .description(entity.description())
                    .aliases(aliasArray)
                    .sourcePages(pageArray)
                    .documentId(fileDetail.getId())
                    .build();
            
            try {
                entityNodeRepository.save(entityNode);
                savedEntityCount++;
                log.debug("保存实体节点成功: id={}, name={}, type={}", nodeId, entity.name(), entity.type());
            } catch (Exception e) {
                log.error("保存实体节点失败: id={}, name={}", nodeId, entity.name(), e);
                throw new BusinessException("保存实体节点失败: " + e.getMessage());
            }

            try {
                neo4jClient.query("""
                        MATCH (d:Document {id: $documentId})
                        MATCH (e:Entity {id: $entityId})
                        MERGE (e)-[:BELONGS_TO]->(d)
                        """)
                        .bind(fileDetail.getId()).to("documentId")
                        .bind(nodeId).to("entityId")
                        .run();
                log.debug("创建BELONGS_TO关系成功: entity={} -> document={}", nodeId, fileDetail.getId());
            } catch (Exception e) {
                log.error("创建BELONGS_TO关系失败: entity={} -> document={}", nodeId, fileDetail.getId(), e);
                throw new BusinessException("创建BELONGS_TO关系失败: " + e.getMessage());
            }

            registerEntityLookup(entityNodeLookup, externalId, nodeId, true);
            registerEntityLookup(entityNodeLookup, normalizedExternalId, nodeId, true);
            registerEntityLookup(entityNodeLookup, entity.name(), nodeId, false);
            if (entity.aliases() != null) {
                for (String alias : entity.aliases()) {
                    registerEntityLookup(entityNodeLookup, alias, nodeId, false);
                }
            }
        }
        
        log.info("保存实体节点完成: fileId={}, 成功数={}/{}", 
            fileDetail.getId(), savedEntityCount, graphData.entities().size());

        // 保存关系
        int savedRelationshipCount = 0;
        for (GraphRelationship relationship : graphData.relationships()) {
            String sourceId = resolveEntityNodeId(entityNodeLookup, relationship.fromId(), relationship.fromName());
            String targetId = resolveEntityNodeId(entityNodeLookup, relationship.toId(), relationship.toName());
            if (!StringUtils.hasText(sourceId) || !StringUtils.hasText(targetId)) {
                log.debug("跳过关系（实体未找到）: from={}/{} -> to={}/{}", 
                    relationship.fromId(), relationship.fromName(), 
                    relationship.toId(), relationship.toName());
                continue;
            }

            String relationshipType = sanitizeRelationshipType(relationship.type());
            List<Integer> sourcePages = relationship.sourcePages() == null
                    ? List.of()
                    : new ArrayList<>(new LinkedHashSet<>(relationship.sourcePages()));
            
            try {
                neo4jClient.query(String.format("""
                    MATCH (source:Entity {id: $sourceId})
                    MATCH (target:Entity {id: $targetId})
                    MERGE (source)-[r:%s]->(target)
                    SET r.description = $description,
                        r.sourcePages = $sourcePages
                    """, relationshipType))
                    .bind(sourceId).to("sourceId")
                    .bind(targetId).to("targetId")
                    .bind(Optional.ofNullable(relationship.description()).orElse(""))
                    .to("description")
                    .bind(sourcePages)
                    .to("sourcePages")
                    .run();
                savedRelationshipCount++;
                log.debug("保存关系成功: {} -[{}]-> {}", sourceId, relationshipType, targetId);
            } catch (Exception e) {
                log.error("保存关系失败: {} -[{}]-> {}", sourceId, relationshipType, targetId, e);
                throw new BusinessException("保存关系失败: " + e.getMessage());
            }
        }
        
        log.info("知识图谱持久化完成: fileId={}, 实体={}/{}, 关系={}/{}", 
            fileDetail.getId(), savedEntityCount, graphData.entities().size(), 
            savedRelationshipCount, graphData.relationships().size());
    }

    private String sanitizeRelationshipType(String type) {
        if (!StringUtils.hasText(type)) {
            return "关联";
        }
        // 保留中文、英文、数字，去除特殊字符
        String sanitized = type.trim().replaceAll("[\\s\\p{Punct}]+", "_");
        return StringUtils.hasText(sanitized) ? sanitized : "关联";
    }

    private String extractJson(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int closingFence = trimmed.lastIndexOf("```");
            if (closingFence > -1) {
                trimmed = trimmed.substring(0, closingFence);
            }
        }
        return trimmed.trim();
    }

    private String toString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode jsonNode) {
            if (jsonNode.isTextual()) {
                return jsonNode.asText();
            }
            return jsonNode.toString();
        }
        return String.valueOf(value);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private FileDetail loadFile(String fileId) {
        FileDetail fileDetail = fileDetailMapper.selectById(fileId);
        if (fileDetail == null) {
            throw new BusinessException("文件不存在: " + fileId);
        }
        return fileDetail;
    }

    private static final class AggregatedEntity {

        private final String canonicalKey;
        private String displayName;
        private final LinkedHashSet<String> names = new LinkedHashSet<>();
        private final LinkedHashSet<String> aliases = new LinkedHashSet<>();
        private final LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        private String preferredId;
        private final LinkedHashMap<String, Integer> typeCounts = new LinkedHashMap<>();
        private final LinkedHashSet<String> descriptions = new LinkedHashSet<>();
        private final TreeSet<Integer> sourcePages = new TreeSet<>();

        AggregatedEntity(String canonicalKey, GraphEntity seed) {
            this.canonicalKey = canonicalKey;
            if (StringUtils.hasText(seed.name())) {
                this.displayName = seed.name().trim();
                names.add(this.displayName);
            } else {
                this.displayName = canonicalKey;
            }
            registerIdentifier(seed.id());
            if (seed.aliases() != null) {
                for (String alias : seed.aliases()) {
                    if (StringUtils.hasText(alias)) {
                        aliases.add(alias.trim());
                    }
                }
            }
        }

        void merge(GraphEntity entity, Set<Integer> chunkPages) {
            registerIdentifier(entity.id());

            if (StringUtils.hasText(entity.name())) {
                String trimmed = entity.name().trim();
                names.add(trimmed);
                if (!StringUtils.hasText(displayName) || displayName.equalsIgnoreCase(canonicalKey)) {
                    displayName = trimmed;
                }
            }

            if (StringUtils.hasText(entity.type())) {
                String type = entity.type().trim();
                typeCounts.merge(type, 1, Integer::sum);
            }

            if (StringUtils.hasText(entity.description())) {
                descriptions.add(entity.description().trim());
            }

            if (entity.aliases() != null) {
                for (String alias : entity.aliases()) {
                    if (StringUtils.hasText(alias)) {
                        aliases.add(alias.trim());
                    }
                }
            }

            if (entity.sourcePages() != null) {
                for (Integer page : entity.sourcePages()) {
                    if (page != null) {
                        sourcePages.add(page);
                    }
                }
            }

            if (chunkPages != null) {
                for (Integer page : chunkPages) {
                    if (page != null) {
                        sourcePages.add(page);
                    }
                }
            }
        }

        private void registerIdentifier(String id) {
            if (!StringUtils.hasText(id)) {
                return;
            }
            String trimmed = id.trim();
            identifiers.add(trimmed);
            if (!StringUtils.hasText(preferredId)) {
                preferredId = trimmed;
            }
        }

        GraphEntity toGraphEntity() {
            String type = resolveType();
            String description = buildDescription();
            LinkedHashSet<String> aliasSet = new LinkedHashSet<>();
            aliasSet.addAll(aliases);
            names.stream()
                    .filter(name -> !name.equals(displayName))
                    .forEach(aliasSet::add);
            List<String> aliasList = aliasSet.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
            List<Integer> pages = new ArrayList<>(sourcePages);
            return new GraphEntity(getPreferredId(), displayName, type, description, aliasList, pages);
        }

        String getPreferredId() {
            return StringUtils.hasText(preferredId) ? preferredId : canonicalKey;
        }

        String getDisplayName() {
            return displayName;
        }

        Set<String> getAllNames() {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            values.add(displayName);
            values.addAll(names);
            values.addAll(aliases);
            return values;
        }

        Set<String> getAllIdentifiers() {
            LinkedHashSet<String> values = new LinkedHashSet<>(identifiers);
            values.add(getPreferredId());
            values.add(canonicalKey);
            return values;
        }

        boolean matches(String candidate) {
            if (!StringUtils.hasText(candidate)) {
                return false;
            }
            String trimmed = candidate.trim();
            if (getAllNames().contains(trimmed) || getAllIdentifiers().contains(trimmed)) {
                return true;
            }
            String normalizedCandidate = normalizeStatic(trimmed);
            for (String name : getAllNames()) {
                if (areSimilarStatic(normalizedCandidate, normalizeStatic(name))) {
                    return true;
                }
            }
            for (String identifier : getAllIdentifiers()) {
                if (areSimilarStatic(normalizedCandidate, normalizeStatic(identifier))) {
                    return true;
                }
            }
            return false;
        }

        private String resolveType() {
            if (typeCounts.isEmpty()) {
                return "concept";
            }
            return typeCounts.entrySet().stream()
                    .sorted((left, right) -> {
                        int countDiff = Integer.compare(right.getValue(), left.getValue());
                        if (countDiff != 0) {
                            return countDiff;
                        }
                        return Integer.compare(right.getKey().length(), left.getKey().length());
                    })
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("concept");
        }

        private String buildDescription() {
            if (descriptions.isEmpty()) {
                return "";
            }
            return descriptions.stream()
                    .filter(StringUtils::hasText)
                    .limit(3)
                    .collect(Collectors.joining("\n"));
        }

        private String normalizeStatic(String value) {
            return value == null ? null : Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ");
        }

        private boolean areSimilarStatic(String left, String right) {
            if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
                return false;
            }
            if (left.equals(right)) {
                return true;
            }
            String compactLeft = left.replaceAll("\\s+", "");
            String compactRight = right.replaceAll("\\s+", "");
            if (compactLeft.equals(compactRight)) {
                return true;
            }
            if (compactLeft.contains(compactRight) || compactRight.contains(compactLeft)) {
                return true;
            }
            int distance = levenshteinStatic(compactLeft, compactRight);
            return distance >= 0 && distance <= 2;
        }

        private int levenshteinStatic(String left, String right) {
            if (left == null || right == null) {
                return -1;
            }
            int[][] dp = new int[left.length() + 1][right.length() + 1];
            for (int i = 0; i <= left.length(); i++) {
                dp[i][0] = i;
            }
            for (int j = 0; j <= right.length(); j++) {
                dp[0][j] = j;
            }
            for (int i = 1; i <= left.length(); i++) {
                for (int j = 1; j <= right.length(); j++) {
                    int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + cost);
                }
            }
            return dp[left.length()][right.length()];
        }
    }

    private static final class AggregatedRelationship {

        private final String fromId;
        private final String toId;
        private final String type;
        private final String fromName;
        private final String toName;
        private final LinkedHashSet<String> descriptions = new LinkedHashSet<>();
        private final TreeSet<Integer> sourcePages = new TreeSet<>();

        AggregatedRelationship(String fromId, String toId, String type, String fromName, String toName) {
            this.fromId = fromId;
            this.toId = toId;
            this.type = type;
            this.fromName = fromName;
            this.toName = toName;
        }

        void merge(GraphRelationship relationship, Set<Integer> chunkPages) {
            if (StringUtils.hasText(relationship.description())) {
                descriptions.add(relationship.description().trim());
            }
            if (relationship.sourcePages() != null) {
                for (Integer page : relationship.sourcePages()) {
                    if (page != null) {
                        sourcePages.add(page);
                    }
                }
            }
            if (chunkPages != null) {
                for (Integer page : chunkPages) {
                    if (page != null) {
                        sourcePages.add(page);
                    }
                }
            }
        }

        GraphRelationship toGraphRelationship() {
            String description = descriptions.stream()
                    .filter(StringUtils::hasText)
                    .limit(3)
                    .collect(Collectors.joining("\n"));
            return new GraphRelationship(fromId, toId, type, description, new ArrayList<>(sourcePages), fromName, toName);
        }
    }

    private record ChunkExtractionResult(Set<Integer> pageNumbers, KnowledgeGraphData data) {
    }

    private record QaPairData(String question, String answer, String sourceText, BigDecimal confidenceScore) {
    }

    private record GraphEntity(String id, String name, String type, String description, List<String> aliases, List<Integer> sourcePages) {
    }

    private record GraphRelationship(String fromId, String toId, String type, String description, List<Integer> sourcePages, String fromName, String toName) {
    }

    private record KnowledgeGraphData(List<GraphEntity> entities, List<GraphRelationship> relationships) {
        static KnowledgeGraphData empty() {
            return new KnowledgeGraphData(List.of(), List.of());
        }
    }
}
