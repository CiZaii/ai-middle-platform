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
 * 文件记录表实体，兼容 X-File-Storage 框架的字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("file_detail")
public class FileDetail {

    @TableId(value = "id")
    private String id;

    @TableField("url")
    private String url;

    @TableField("size")
    private Long size;

    @TableField("filename")
    private String filename;

    @TableField("original_filename")
    private String originalFilename;

    @TableField("base_path")
    private String basePath;

    @TableField("path")
    private String path;

    @TableField("ext")
    private String ext;

    @TableField("content_type")
    private String contentType;

    @TableField("platform")
    private String platform;

    @TableField("th_url")
    private String thUrl;

    @TableField("th_filename")
    private String thFilename;

    @TableField("th_size")
    private Long thSize;

    @TableField("th_content_type")
    private String thContentType;

    @TableField("object_id")
    private String objectId;

    @TableField("object_type")
    private String objectType;

    @TableField("metadata")
    private String metadata;

    @TableField("user_metadata")
    private String userMetadata;

    @TableField("th_metadata")
    private String thMetadata;

    @TableField("th_user_metadata")
    private String thUserMetadata;

    @TableField("attr")
    private String attr;

    @TableField("file_acl")
    private String fileAcl;

    @TableField("th_file_acl")
    private String thFileAcl;

    @TableField("hash_info")
    private String hashInfo;

    @TableField("upload_id")
    private String uploadId;

    @TableField("upload_status")
    private Integer uploadStatus;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
