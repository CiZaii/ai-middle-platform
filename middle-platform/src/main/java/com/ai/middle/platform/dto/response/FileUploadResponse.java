package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件访问URL
     */
    private String url;

    /**
     * 文件大小
     */
    private Long size;
}
