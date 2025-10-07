package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问答对DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaPairDTO {

    /**
     * 问答对ID
     */
    private String id;

    /**
     * 问题内容
     */
    private String question;

    /**
     * 答案内容
     */
    private String answer;

    /**
     * 来源文本
     */
    private String sourceText;
}
