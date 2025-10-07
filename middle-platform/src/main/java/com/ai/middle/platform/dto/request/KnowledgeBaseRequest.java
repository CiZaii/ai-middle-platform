package com.ai.middle.platform.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 知识库创建或更新请求DTO
 */
@Data
public class KnowledgeBaseRequest {

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
    private String name;

    /**
     * 知识库描述
     */
    private String description;
}
