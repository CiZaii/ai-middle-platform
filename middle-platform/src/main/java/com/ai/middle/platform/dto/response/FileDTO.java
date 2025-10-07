package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {

    /**
     * 文件唯一标识
     */
    private String id;

    /**
     * 所属知识库ID
     */
    private String knowledgeBaseId;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件类型（业务分类）
     */
    private String type;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 文件大小
     */
    private Long size;

    /**
     * 文件访问地址
     */
    private String url;

    /**
     * 缩略图地址
     */
    private String thumbnailUrl;

    /**
     * 上传时间
     */
    private LocalDateTime uploadedAt;

    /**
     * 上传者信息
     */
    private UserDTO uploadedBy;

    /**
     * 文件各项处理状态
     */
    private FileStatusesDTO statuses;

    /**
     * 文件标签（AI自动生成）
     */
    private java.util.List<String> tags;

    /**
     * 错误信息
     */
    private String errorMessage;
}
