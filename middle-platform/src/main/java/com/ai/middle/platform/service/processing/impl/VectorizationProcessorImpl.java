package com.ai.middle.platform.service.processing.impl;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
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
public class VectorizationProcessorImpl implements com.ai.middle.platform.service.processing.VectorizationProcessor {

    private static final int DEFAULT_CHUNK_SIZE = 2000;
    private static final int DEFAULT_OVERLAP = 200;
    private static final int MIN_CHUNK_SIZE = 200;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    @Override
    public void vectorize(String fileId, String content, Integer chunkSize, Integer overlap) {
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

        List<TextSegment> segments = documents.stream().map(Document::toTextSegment).toList();
        Response<List<Embedding>> response = embeddingModel.embedAll(segments);
        List<Embedding> embeddings = response != null ? response.content() : null;
        if (embeddings == null || embeddings.size() != segments.size()) {
            log.warn("向量化结果数量不匹配: fileId={}, segments={}, embeddings={}",
                    fileId, segments.size(), embeddings != null ? embeddings.size() : 0);
            return;
        }

        embeddingStore.addAll(embeddings, segments);
        log.info("向量化完成: fileId={}, chunks={}", fileId, documents.size());
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
}

