package com.ai.middle.platform.service.processing.impl;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.common.util.IdGenerator;
import com.ai.middle.platform.entity.po.FileDetail;
import com.ai.middle.platform.entity.po.KbQaPair;
import com.ai.middle.platform.repository.mapper.FileDetailMapper;
import com.ai.middle.platform.repository.mapper.KbQaPairMapper;
import com.ai.middle.platform.service.PromptService;
import com.ai.middle.platform.service.processing.ChatExecutor;
import com.ai.middle.platform.service.processing.QaGenerationProcessor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class QaGenerationProcessorImpl implements QaGenerationProcessor {

    private static final String BUSINESS_QA = "qa";
    private static final int MIN_SEGMENT_LENGTH = 100;
    private static final int IDEAL_SEGMENT_LENGTH = 400;
    private static final int MAX_SEGMENT_LENGTH = 1200;
    private static final int DEFAULT_QA_MAX = 100;
    private static final BigDecimal DEFAULT_QA_CONFIDENCE = BigDecimal.valueOf(0.9);

    private final FileDetailMapper fileDetailMapper;
    private final KbQaPairMapper qaPairMapper;
    private final PromptService promptService;
    private final ChatExecutor chatExecutor;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStoreQa;

    @Override
    public void generate(String fileId, String content, Integer maxPairs) {
        log.info("开始生成问答对: fileId={}", fileId);
        if (!StringUtils.hasText(content)) {
            log.warn("问答对生成内容为空，跳过: fileId={}", fileId);
            return;
        }

        FileDetail fileDetail = loadFile(fileId);
        String fileName = StringUtils.hasText(fileDetail.getOriginalFilename())
                ? fileDetail.getOriginalFilename()
                : fileDetail.getFilename();

        log.info("========================================");
        log.info("开始问答对生成任务");
        log.info("文件名称: {}", fileName);
        log.info("文件ID: {}", fileId);
        log.info("内容长度: {} 字符", content.length());
        log.info("========================================");

        // 删除旧的问答对
        qaPairMapper.delete(new LambdaQueryWrapper<KbQaPair>()
                .eq(KbQaPair::getFileId, fileDetail.getId()));

        List<MarkdownSegment> segments = splitMarkdownIntoSegments(content);
        if (segments.isEmpty()) {
            log.warn("文本分段为空: fileId={}", fileId);
            return;
        }

        int totalSegments = segments.size();
        int totalChars = segments.stream().mapToInt(s -> s.content().length()).sum();
        log.info("Markdown分段完成: 共 {} 个语义段落，总字符数 {}", totalSegments, totalChars);

        List<QaPairData> allQaPairs = new ArrayList<>();
        int processedSegments = 0;

        for (int i = 0; i < segments.size(); i++) {
            MarkdownSegment mdSegment = segments.get(i);
            String segment = mdSegment.content();
            if (!StringUtils.hasText(segment) || segment.length() < MIN_SEGMENT_LENGTH) {
                log.debug("跳过段落 {}/{}: 内容过短 ({} 字符)", i + 1, totalSegments, segment.length());
                continue;
            }
            try {
                processedSegments++;
                int questionsToGenerate = calculateQuestionCount(segment.length());

                String promptTemplate = promptService.getActivePromptContent(BUSINESS_QA);
                Map<String, Object> variables = new HashMap<>();
                variables.put("questionCount", questionsToGenerate);
                variables.put("content", segment);
                String prompt = promptService.formatPrompt(promptTemplate, variables);

                String response = chatExecutor.execute(BUSINESS_QA, (chatModel, runtimeConfig) -> chatModel.generate(prompt));
                List<QaPairData> segmentQaPairs = parseQaPairs(response, fileId);
                for (QaPairData qaPair : segmentQaPairs) {
                    allQaPairs.add(new QaPairData(
                            qaPair.question(),
                            qaPair.answer(),
                            segment,
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
            log.warn("问答对生成任务结束: 未生成任何问答对, fileId={}", fileId);
            return;
        }

        int targetPairs = Optional.ofNullable(maxPairs).filter(value -> value > 0).orElse(DEFAULT_QA_MAX);
        List<QaPairData> finalQaPairs = allQaPairs.size() > targetPairs ? allQaPairs.subList(0, targetPairs) : allQaPairs;

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

        // QA向量写入：text=question
        try {
            List<dev.langchain4j.data.document.Document> qaDocuments = new ArrayList<>();
            for (QaPairData qaPair : finalQaPairs) {
                if (!StringUtils.hasText(qaPair.question())) {
                    continue;
                }
                Map<String, Object> md = new HashMap<>();
                md.put("fileId", fileDetail.getId());
                md.put("documentId", fileDetail.getId());
                md.put("fileName", fileName);
                qaDocuments.add(new dev.langchain4j.data.document.Document(qaPair.question(), Metadata.from(md)));
            }
            if (!qaDocuments.isEmpty()) {
                List<TextSegment> qaSegments = qaDocuments.stream().map(dev.langchain4j.data.document.Document::toTextSegment).toList();
                Response<List<Embedding>> qaEmbeddingsResp = embeddingModel.embedAll(qaSegments);
                List<Embedding> qaEmbeddings = qaEmbeddingsResp != null ? qaEmbeddingsResp.content() : null;
                if (qaEmbeddings != null && qaEmbeddings.size() == qaSegments.size()) {
                    embeddingStoreQa.addAll(qaEmbeddings, qaSegments);
                    log.info("QA向量写入完成: fileId={}, count={}", fileId, qaSegments.size());
                } else {
                    log.warn("QA向量数量不匹配或为空: segments={}, embeddings={}", qaSegments.size(), qaEmbeddings != null ? qaEmbeddings.size() : 0);
                }
            } else {
                log.warn("没有可写入向量库的QA问题: fileId={}", fileId);
            }
        } catch (Exception ex) {
            log.error("写入QA向量库失败: fileId={}, error={}", fileId, ex.getMessage(), ex);
        }

        log.info("问答对生成任务完成: 保存问答对 {} 个", savedCount);
    }

    private FileDetail loadFile(String fileId) {
        FileDetail fileDetail = fileDetailMapper.selectById(fileId);
        if (fileDetail == null) {
            throw new BusinessException("文件不存在: " + fileId);
        }
        return fileDetail;
    }

    private record MarkdownSegment(String type, String content) {
    }

    private record QaPairData(String question, String answer, String sourceText, BigDecimal confidenceScore) {
    }

    private List<QaPairData> parseQaPairs(String response, String fileId) {
        String json = extractJson(response);
        if (!StringUtils.hasText(json)) {
            log.warn("问答对响应为空或格式不正确: fileId={}", fileId);
            return List.of();
        }
        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            List<QaPairData> results = new ArrayList<>();
            for (Map<String, Object> item : rawList) {
                String question = toString(item.get("question"));
                String answer = toString(item.get("answer"));
                if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
                    continue;
                }
                String sourceText = Optional.ofNullable(toString(item.get("sourceText")))
                        .orElseGet(() -> toString(item.get("source_text")));
                BigDecimal confidence = toBigDecimal(Optional.ofNullable(item.get("confidenceScore")).orElse(item.get("confidence_score")));
                results.add(new QaPairData(question, answer, sourceText, confidence));
            }
            return results;
        } catch (Exception e) {
            log.error("解析问答对结果失败: fileId={} response={}", fileId, response, e);
            return List.of();
        }
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

    private int calculateQuestionCount(int segmentLength) {
        if (segmentLength < 200) {
            return 3;
        } else if (segmentLength < 400) {
            return 5;
        } else if (segmentLength < 600) {
            return 7;
        } else if (segmentLength < 1000) {
            return 10;
        } else {
            return 12;
        }
    }

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

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("```") ) {
                if (inCodeBlock) {
                    currentSegment.append(line).append("\n");
                    addSegmentIfValid(segments, currentSegment.toString(), "代码块");
                    currentSegment = new StringBuilder();
                    inCodeBlock = false;
                } else {
                    if (currentSegment.length() > 0) {
                        addSegmentIfValid(segments, currentSegment.toString(), currentType);
                        currentSegment = new StringBuilder();
                    }
                    currentSegment.append(line).append("\n");
                    inCodeBlock = true;
                    currentType = "代码块";
                }
                continue;
            }
            if (inCodeBlock) {
                currentSegment.append(line).append("\n");
                continue;
            }

            if (trimmedLine.matches("^#{1,6}\\s+.+")) {
                if (currentSegment.length() > 0) {
                    addSegmentIfValid(segments, currentSegment.toString(), currentType);
                    currentSegment = new StringBuilder();
                }
                currentType = "标题段落";
                currentSegment.append(line).append("\n");
                continue;
            }

            if (trimmedLine.startsWith("|") && trimmedLine.endsWith("|")) {
                if (!inTable) {
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
                addSegmentIfValid(segments, currentSegment.toString(), currentType);
                currentSegment = new StringBuilder();
                inTable = false;
                currentType = "paragraph";
            }

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
                addSegmentIfValid(segments, currentSegment.toString(), currentType);
                currentSegment = new StringBuilder();
                currentType = "paragraph";
                continue;
            }

            if (!StringUtils.hasText(trimmedLine)) {
                if (currentSegment.length() > 0 && !currentType.equals("列表")) {
                    addSegmentIfValid(segments, currentSegment.toString(), currentType);
                    currentSegment = new StringBuilder();
                    currentType = "paragraph";
                }
                continue;
            }

            currentSegment.append(line).append("\n");
            if (currentSegment.length() > MAX_SEGMENT_LENGTH && !inCodeBlock && !inTable) {
                addSegmentIfValid(segments, currentSegment.toString(), currentType);
                currentSegment = new StringBuilder();
            }
        }

        if (currentSegment.length() > 0) {
            addSegmentIfValid(segments, currentSegment.toString(), currentType);
        }
        return mergeShortSegments(segments);
    }

    private void addSegmentIfValid(List<MarkdownSegment> segments, String content, String type) {
        String trimmed = content.trim();
        if (trimmed.length() >= MIN_SEGMENT_LENGTH) {
            segments.add(new MarkdownSegment(type, trimmed));
        } else if (trimmed.length() > 20) {
            segments.add(new MarkdownSegment(type, trimmed));
        }
    }

    private List<MarkdownSegment> mergeShortSegments(List<MarkdownSegment> segments) {
        if (segments.isEmpty()) {
            return segments;
        }
        List<MarkdownSegment> merged = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String bufferType = segments.get(0).type();
        for (MarkdownSegment segment : segments) {
            if (segment.type().equals("代码块") || segment.type().equals("表格")) {
                if (buffer.length() > 0) {
                    merged.add(new MarkdownSegment(bufferType, buffer.toString().trim()));
                    buffer = new StringBuilder();
                }
                merged.add(segment);
                bufferType = "paragraph";
                continue;
            }
            if (buffer.length() == 0) {
                buffer.append(segment.content());
                bufferType = segment.type();
            } else if (buffer.length() + segment.content().length() < IDEAL_SEGMENT_LENGTH) {
                buffer.append("\n\n").append(segment.content());
            } else {
                merged.add(new MarkdownSegment(bufferType, buffer.toString().trim()));
                buffer = new StringBuilder(segment.content());
                bufferType = segment.type();
            }
        }
        if (buffer.length() > 0) {
            merged.add(new MarkdownSegment(bufferType, buffer.toString().trim()));
        }
        return merged;
    }
}
