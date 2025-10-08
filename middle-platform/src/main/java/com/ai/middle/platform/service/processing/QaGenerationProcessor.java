package com.ai.middle.platform.service.processing;

public interface QaGenerationProcessor {
    void generate(String fileId, String content, Integer maxPairs);
}

