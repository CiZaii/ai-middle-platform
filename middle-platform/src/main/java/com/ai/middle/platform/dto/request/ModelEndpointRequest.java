package com.ai.middle.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 端点配置创建/更新请求
 */
@Data
public class ModelEndpointRequest {

    /**
     * 端点名称
     */
    @NotBlank(message = "端点名称不能为空")
    private String name;

    /**
     * 基础URL
     */
    @NotBlank(message = "Base URL不能为空")
    private String baseUrl;

    /**
     * 提供商
     */
    @NotBlank(message = "提供商不能为空")
    private String provider;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否启用
     */
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    /**
     * 关联业务ID列表
     */
    private List<Long> businessIds;

    /**
     * 支持的模型名称列表
     */
    private List<String> modelNames;
}
