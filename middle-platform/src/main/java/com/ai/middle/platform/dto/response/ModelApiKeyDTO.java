package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API Key配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelApiKeyDTO {

    private Long id;
    private String keyId;
    private Long endpointId;
    private String name;
    private String apiKey;
    private String displayKey;
    private Boolean enabled;
    private Integer rateLimitPerMinute;
    private Integer rateLimitPerDay;
    private Long totalRequests;
    private Long successRequests;
    private Long failedRequests;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private UserDTO createdBy;
    private EndpointSummary endpoint;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointSummary {
        private Long id;
        private String name;
        private String provider;
        private String baseUrl;
    }
}
