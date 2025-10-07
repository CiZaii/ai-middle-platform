package com.ai.middle.platform.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("model_api_key")
public class ModelApiKey {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("key_id")
    private String keyId;

    @TableField("endpoint_id")
    private Long endpointId;

    @TableField("name")
    private String name;

    @TableField("api_key")
    private String apiKey;

    @TableField("display_key")
    private String displayKey;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    @TableField("rate_limit_per_day")
    private Integer rateLimitPerDay;

    @TableField("total_requests")
    private Long totalRequests;

    @TableField("success_requests")
    private Long successRequests;

    @TableField("failed_requests")
    private Long failedRequests;

    @TableField("last_error")
    private String lastError;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
