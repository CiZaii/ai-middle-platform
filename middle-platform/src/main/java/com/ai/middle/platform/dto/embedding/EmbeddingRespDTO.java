package com.ai.middle.platform.dto.embedding;

import lombok.Data;

import java.util.List;

@Data
public class EmbeddingRespDTO {

    private List<EmbeddingData> data;
    private String model;
    private Usage usage;

    @Data
    public static class EmbeddingData {
        private List<Double> embedding;
        private Integer index;
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer totalTokens;
    }
}
