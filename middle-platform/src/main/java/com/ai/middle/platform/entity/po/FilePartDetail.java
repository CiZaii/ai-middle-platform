package com.ai.middle.platform.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件分片持久化实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("file_part_detail")
public class FilePartDetail {

    @TableId(value = "id")
    private String id;

    @TableField("platform")
    private String platform;

    @TableField("upload_id")
    private String uploadId;

    @TableField("e_tag")
    private String eTag;

    @TableField("part_number")
    private Integer partNumber;

    @TableField("part_size")
    private Long partSize;

    @TableField("hash_info")
    private String hashInfo;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
