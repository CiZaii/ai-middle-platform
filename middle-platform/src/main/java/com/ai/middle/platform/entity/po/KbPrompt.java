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

/**
 * Prompt 模板管理实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("kb_prompt")
public class KbPrompt {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("prompt_id")
    private String promptId;

    @TableField("business_code")
    private String businessCode;

    @TableField("prompt_name")
    private String promptName;

    @TableField("prompt_content")
    private String promptContent;

    @TableField("description")
    private String description;

    @TableField("variables")
    private String variables;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("version")
    private Integer version;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
