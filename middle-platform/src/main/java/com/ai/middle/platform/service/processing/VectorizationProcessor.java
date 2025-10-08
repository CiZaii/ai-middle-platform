package com.ai.middle.platform.service.processing;

public interface VectorizationProcessor {
    void vectorize(String fileId, String content, Integer chunkSize, Integer overlap);
}

