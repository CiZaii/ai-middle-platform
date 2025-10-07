package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 业务配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelBusinessDTO {

    private Long id;
    private String businessId;
    private String name;
    private String code;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserDTO createdBy;
}
