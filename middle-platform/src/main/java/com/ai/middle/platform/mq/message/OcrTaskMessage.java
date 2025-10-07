package com.ai.middle.platform.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * OCR任务消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 页级文档ID
     */
    private String documentId;

    /**
     * 页码 (从1开始)
     */
    private Integer pageIndex;

    /**
     * 页图片URL
     */
    private String imageUrl;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 知识库ID (deprecated)
     */
    @Deprecated
    private String kbId;

    /**
     * 触发任务的用户ID (deprecated)
     */
    @Deprecated
    private Long userId;

    /**
     * 文件访问地址 (deprecated)
     */
    @Deprecated
    private String filePath;
}
