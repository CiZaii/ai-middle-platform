package com.ai.middle.platform.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 向量化任务消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorizationTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 文件ID */
    private String fileId;

    /** OCR识别结果 */
    private String ocrContent;

    /** 分块大小 */
    private Integer chunkSize;

    /** 分块重叠大小 */
    private Integer overlap;
}
