package com.ai.middle.platform.controller;

import com.ai.middle.platform.common.constant.ApiConstants;
import com.ai.middle.platform.common.result.Result;
import com.ai.middle.platform.dto.response.FileDTO;
import com.ai.middle.platform.dto.response.FileDetailDTO;
import com.ai.middle.platform.dto.response.FileUploadResponse;
import com.ai.middle.platform.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件控制器
 */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 查询知识库的文件列表
     */
    @GetMapping(ApiConstants.KB_PATH + "/{kbId}/files")
    public Result<List<FileDTO>> listByKbId(@PathVariable String kbId) {
        List<FileDTO> files = fileService.listByKbId(kbId);
        return Result.success(files);
    }

    /**
     * 查询文件详情
     */
    @GetMapping(ApiConstants.FILE_PATH + "/{id}")
    public Result<FileDetailDTO> getById(@PathVariable String id) {
        FileDetailDTO file = fileService.getDetailById(id);
        return Result.success(file);
    }

    /**
     * 上传文件
     */
    @PostMapping(ApiConstants.KB_PATH + "/{kbId}/files")
    public Result<FileUploadResponse> upload(
            @PathVariable String kbId,
            @RequestParam("file") MultipartFile file) {
        FileUploadResponse response = fileService.upload(kbId, file);
        return Result.success(response);
    }

    /**
     * 删除文件
     */
    @DeleteMapping(ApiConstants.FILE_PATH + "/{id}")
    public Result<Void> delete(@PathVariable String id) {
        fileService.delete(id);
        return Result.success();
    }

    /**
     * 手动触发处理任务
     */
    @PostMapping(ApiConstants.FILE_PATH + "/{id}/process/{type}")
    public Result<Void> triggerProcess(
            @PathVariable String id,
            @PathVariable String type) {
        fileService.triggerProcess(id, type);
        return Result.success();
    }

    /**
     * 根据标签搜索文件
     */
    @GetMapping(ApiConstants.KB_PATH + "/{kbId}/files/search/by-tag")
    public Result<List<FileDTO>> searchByTag(
            @PathVariable String kbId,
            @RequestParam String tag) {
        List<FileDTO> files = fileService.searchByTag(kbId, tag);
        return Result.success(files);
    }

    /**
     * 更新文件的OCR内容
     */
    @PutMapping(ApiConstants.FILE_PATH + "/{id}/ocr-content")
    public Result<Void> updateOcrContent(
            @PathVariable String id,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> payload) {
        String content = payload.get("content");
        fileService.updateOcrContent(id, content);
        return Result.success();
    }
}
