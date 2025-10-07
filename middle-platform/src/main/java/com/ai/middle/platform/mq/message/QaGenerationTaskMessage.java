package com.ai.middle.platform.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 问答对生成任务消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaGenerationTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 文件ID */
    private String fileId;

    /** OCR识别结果 */
    private String ocrContent;

    /** 最大问答对数量 */
    private Integer maxPairs;
}
