package com.ai.middle.platform.dto.embedding;

import lombok.Data;

@Data
public class EmbeddingReqDTO {
    private String input;
    private String model;
}
