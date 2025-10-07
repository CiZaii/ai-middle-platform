package com.ai.middle.platform.entity.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 附加在 file_detail.attr 字段中的业务元数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDetailAttributes {

    /** 上传者用户ID */
    private Long uploadedBy;

    /** 业务文件类型（如 pdf、word、image 等） */
    private String fileType;

    /** OCR 处理状态 */
    private String ocrStatus;

    /** 向量化处理状态 */
    private String vectorizationStatus;

    /** 问答对生成状态 */
    private String qaPairsStatus;

    /** 知识图谱生成状态 */
    private String knowledgeGraphStatus;

    /** 文件标签（AI自动生成，约5个）*/
    private List<String> tags;

    /** 错误信息 */
    private String errorMessage;
}
