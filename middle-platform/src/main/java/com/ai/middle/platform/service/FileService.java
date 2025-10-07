package com.ai.middle.platform.service;

import com.ai.middle.platform.dto.response.FileDTO;
import com.ai.middle.platform.dto.response.FileDetailDTO;
import com.ai.middle.platform.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 查询指定知识库下的文件列表
     */
    List<FileDTO> listByKbId(String kbId);

    /**
     * 查询文件详情
     */
    FileDetailDTO getDetailById(String id);

    /**
     * 上传文件
     */
    FileUploadResponse upload(String kbId, MultipartFile file);

    /**
     * 删除文件
     */
    void delete(String id);

    /**
     * 手动触发文件处理流程
     */
    void triggerProcess(String fileId, String processType);

    /**
     * 根据标签搜索文件
     */
    List<FileDTO> searchByTag(String kbId, String tag);

    /**
     * 更新文件的OCR内容
     */
    void updateOcrContent(String fileId, String content);
}
