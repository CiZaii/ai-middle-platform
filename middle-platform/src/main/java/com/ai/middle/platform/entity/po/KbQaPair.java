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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("kb_qa_pair")
public class KbQaPair {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("qa_id")
    private String qaId;

    @TableField("file_id")
    private String fileId;

    @TableField("question")
    private String question;

    @TableField("answer")
    private String answer;

    @TableField("source_text")
    private String sourceText;

    @TableField("confidence_score")
    private BigDecimal confidenceScore;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
