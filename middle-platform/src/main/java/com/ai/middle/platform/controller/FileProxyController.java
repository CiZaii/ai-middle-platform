package com.ai.middle.platform.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 文件代理控制器 - 通过x-file-storage获取文件，解决前端CORS问题
 */
@Slf4j
@RestController
@RequestMapping("/api/proxy")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FileProxyController {
    
    private final FileStorageService fileStorageService;

    /**
     * 代理文件访问 - 使用x-file-storage下载文件
     * @param url 文件URL（来自MinIO/OSS）
     * @return 文件内容
     */
    @GetMapping("/file")
    public ResponseEntity<byte[]> proxyFile(@RequestParam String url) {
        log.info("代理文件访问（x-file-storage）: {}", url);
        
        try {
            // 使用x-file-storage的download方法
            byte[] fileContent = fileStorageService.download(url).bytes();
            
            if (fileContent == null || fileContent.length == 0) {
                log.error("文件内容为空: {}", url);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found or empty".getBytes());
            }
            
            // 根据URL后缀判断Content-Type
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.endsWith(".pdf")) {
                contentType = MediaType.APPLICATION_PDF_VALUE;
            } else if (lowerUrl.matches(".*\\.(jpg|jpeg)$")) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            } else if (lowerUrl.endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG_VALUE;
            } else if (lowerUrl.endsWith(".gif")) {
                contentType = MediaType.IMAGE_GIF_VALUE;
            } else if (lowerUrl.endsWith(".webp")) {
                contentType = "image/webp";
            }
            
            // 构建响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileContent.length);
            headers.setCacheControl(CacheControl.maxAge(3600, java.util.concurrent.TimeUnit.SECONDS));
            // 添加CORS头
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.set("Access-Control-Allow-Headers", "*");
            
            log.info("文件代理成功: {} bytes, contentType={}", fileContent.length, contentType);
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("文件代理失败: {}", e.getMessage(), e);
            String errorMsg = "Failed to proxy file: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMsg.getBytes());
        }
    }
}
