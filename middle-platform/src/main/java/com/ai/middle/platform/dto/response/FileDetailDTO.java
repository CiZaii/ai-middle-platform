package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件详情响应DTO（包含OCR内容、知识图谱、问答对）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class FileDetailDTO extends FileDTO {

    /**
     * OCR识别内容
     */
    private String ocrContent;

    /**
     * 知识图谱数据
     */
    private KnowledgeGraphDTO knowledgeGraph;

    /**
     * 关联问答对
     */
    private List<QaPairDTO> qaPairs;
}
