package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 端点配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelEndpointDTO {

    private Long id;
    private String endpointId;
    private String name;
    private String baseUrl;
    private String provider;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserDTO createdBy;
    private List<ModelInfoDTO> models;
    private List<ModelBusinessDTO> businesses;
    private EndpointStatsDTO stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfoDTO {
        private String id;
        private String name;
        private String provider;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointStatsDTO {
        private long totalApiKeys;
        private long activeApiKeys;
        private long totalRequests;
        private double successRate;
    }
}
