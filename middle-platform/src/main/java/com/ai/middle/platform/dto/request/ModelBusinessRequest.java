package com.ai.middle.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 业务配置创建/更新请求
 */
@Data
public class ModelBusinessRequest {

    /**
     * 业务名称
     */
    @NotBlank(message = "业务名称不能为空")
    private String name;

    /**
     * 业务编码
     */
    @NotBlank(message = "业务编码不能为空")
    private String code;

    /**
     * 业务描述
     */
    private String description;

    /**
     * 是否启用
     */
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
