package com.ai.middle.platform.service.impl;

import com.ai.middle.platform.service.AIProcessService;
import com.ai.middle.platform.service.processing.KnowledgeGraphProcessor;
import com.ai.middle.platform.service.processing.OcrProcessor;
import com.ai.middle.platform.service.processing.QaGenerationProcessor;
import com.ai.middle.platform.service.processing.TagProcessor;
import com.ai.middle.platform.service.processing.VectorizationProcessor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIProcessServiceImpl implements AIProcessService {

    private final OcrProcessor ocrProcessor;
    private final VectorizationProcessor vectorizationProcessor;
    private final QaGenerationProcessor qaGenerationProcessor;
    private final KnowledgeGraphProcessor knowledgeGraphProcessor;
    private final TagProcessor tagProcessor;

    @Override
    public String performOcr(String filePath, String fileType) {
        return ocrProcessor.performOcr(filePath, fileType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void performVectorization(String fileId, String content, Integer chunkSize, Integer overlap) {
        vectorizationProcessor.vectorize(fileId, content, chunkSize, overlap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateQaPairs(String fileId, String content, Integer maxPairs) {
        qaGenerationProcessor.generate(fileId, content, maxPairs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateKnowledgeGraph(String fileId, String content) {
        knowledgeGraphProcessor.generate(fileId, content);
    }

    @Override
    public List<String> generateTags(String fileId, String fileName, String content) {
        return tagProcessor.generate(fileId, fileName, content);
    }
}

