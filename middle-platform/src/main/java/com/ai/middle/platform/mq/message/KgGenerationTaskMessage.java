package com.ai.middle.platform.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 知识图谱生成任务消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KgGenerationTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 文件ID */
    private String fileId;

    /** OCR识别结果 */
    private String ocrContent;

    /** 是否抽取实体 */
    private Boolean extractEntities;

    /** 是否抽取关系 */
    private Boolean extractRelations;
}
