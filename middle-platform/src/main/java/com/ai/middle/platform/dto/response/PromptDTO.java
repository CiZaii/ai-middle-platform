package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt 响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptDTO {

    private Long id;
    private String promptId;
    private String businessCode;
    private String promptName;
    private String promptContent;
    private String description;
    private List<String> variables;
    private Boolean isActive;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
