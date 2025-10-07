package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件处理状态DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileStatusesDTO {

    /**
     * OCR处理状态
     */
    private String ocr;

    /**
     * 向量化处理状态
     */
    private String vectorization;

    /**
     * 问答对生成状态
     */
    private String qaPairs;

    /**
     * 知识图谱生成状态
     */
    private String knowledgeGraph;
}
