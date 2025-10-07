package com.ai.middle.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * API Key创建/更新请求
 */
@Data
public class ModelApiKeyRequest {

    /**
     * 关联端点ID
     */
    @NotNull(message = "端点ID不能为空")
    private Long endpointId;

    /**
     * Key名称
     */
    @NotBlank(message = "Key名称不能为空")
    private String name;

    /**
     * API Key明文
     */
    private String apiKey;

    /**
     * 是否启用
     */
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    /**
     * 每分钟请求限制
     */
    private Integer requestsPerMinute;

    /**
     * 每日请求限制
     */
    private Integer requestsPerDay;

    /**
     * 过期日期
     */
    private LocalDate expiresAt;
}
