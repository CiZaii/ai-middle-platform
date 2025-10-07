package com.ai.middle.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * Prompt 请求 DTO
 */
@Data
public class PromptRequest {

    @NotBlank(message = "业务代码不能为空")
    private String businessCode;

    @NotBlank(message = "Prompt名称不能为空")
    private String promptName;

    @NotBlank(message = "Prompt内容不能为空")
    private String promptContent;

    private String description;

    private String variables;

    @NotNull(message = "是否激活不能为空")
    private Boolean isActive;
}
