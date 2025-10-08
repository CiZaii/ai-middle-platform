package com.ai.middle.platform.service.processing.impl;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.service.PromptService;
import com.ai.middle.platform.service.processing.ChatExecutor;
import com.ai.middle.platform.service.processing.OcrProcessor;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrProcessorImpl implements OcrProcessor {

    private static final String BUSINESS_OCR = "ocr";

    private final FileStorageService fileStorageService;
    private final PromptService promptService;
    private final ChatExecutor chatExecutor;

    @Override
    public String performOcr(String filePath, String fileType) {
        if (!StringUtils.hasText(filePath)) {
            throw new BusinessException("文件路径不能为空");
        }

        String prompt = promptService.getActivePromptContent(BUSINESS_OCR);
        log.debug("开始OCR处理: filePath={}, fileType={}", filePath, fileType);

        FileInfo pageFileInfo = null;
        try {
            pageFileInfo = fileStorageService.getFileInfoByUrl(filePath);
            log.debug("成功获取文件信息: {}", pageFileInfo != null ? "有效" : "无效");
        } catch (Exception ex) {
            log.debug("无法通过URL获取文件信息: {}", filePath, ex);
        }

        try {
            byte[] data;
            if (pageFileInfo != null) {
                data = fileStorageService.download(pageFileInfo).bytes();
            } else {
                if (!isValidFileUrl(filePath)) {
                    throw new BusinessException("无效的文件路径格式: " + filePath +
                            "。请确保路径包含有效的协议前缀（如 http://, https://, file:// 等）");
                }
                data = fileStorageService.download(filePath).bytes();
            }

            if (data == null || data.length == 0) {
                throw new BusinessException("文件内容为空: " + filePath);
            }

            MimeType mimeType = resolveMimeType(fileType);
            String fileName = resolveFileName(filePath);
            String base64Data = Base64.getEncoder().encodeToString(data);
            Image image = Image.builder()
                    .base64Data(base64Data)
                    .mimeType(mimeType.toString())
                    .build();

            UserMessage userMessage = UserMessage.from(List.of(
                    TextContent.from(prompt),
                    ImageContent.from(image)
            ));

            log.debug("准备发送文件到AI模型，mimeType: {}, filename: {}", mimeType, fileName);

            String content = chatExecutor.execute(BUSINESS_OCR, (chatModel, runtimeConfig) -> {
                Response<AiMessage> response = chatModel.generate(userMessage);
                AiMessage aiMessage = response != null ? response.content() : null;
                return aiMessage != null ? aiMessage.text() : null;
            });
            return cleanOcrContent(content);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件下载失败: filePath={}, error={}", filePath, e.getMessage(), e);
            throw new BusinessException("文件下载失败: " + e.getMessage() + "。请检查文件路径格式是否正确");
        }
    }

    private MimeType resolveMimeType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }
        String normalized = fileType.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pdf" -> MimeTypeUtils.parseMimeType("application/pdf");
            case "word", "doc", "docx" -> MimeTypeUtils.parseMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "image", "jpg", "jpeg" -> MimeTypeUtils.IMAGE_JPEG;
            case "png" -> MimeTypeUtils.IMAGE_PNG;
            case "txt", "text" -> MimeTypeUtils.TEXT_PLAIN;
            default -> MimeTypeUtils.APPLICATION_OCTET_STREAM;
        };
    }

    private String cleanOcrContent(String content) {
        if (content == null) {
            return null;
        }
        String sanitized = org.springframework.util.StringUtils.trimWhitespace(content);
        sanitized = removeOcrAssistantPrefix(sanitized);
        sanitized = removeMarkdownCodeFenceWrapper(sanitized);
        return org.springframework.util.StringUtils.trimWhitespace(sanitized);
    }

    private String removeOcrAssistantPrefix(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        List<String> prefixes = List.of(
                "作为一个专业的OCR识别助手，我已识别图片中的所有文字内容，并严格按照您的要求进行格式化。",
                "好的，这是对图片内容的OCR识别结果，已按照您的要求格式化为Markdown。"
        );
        String sanitized = content;
        for (String prefix : prefixes) {
            if (sanitized.startsWith(prefix)) {
                sanitized = sanitized.substring(prefix.length());
                sanitized = org.springframework.util.StringUtils.trimLeadingWhitespace(sanitized);
                break;
            }
        }
        return sanitized;
    }

    private String removeMarkdownCodeFenceWrapper(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        String sanitized = org.springframework.util.StringUtils.trimWhitespace(content);
        if (!sanitized.startsWith("```")) {
            return sanitized;
        }
        int newlineIndex = sanitized.indexOf('\n');
        if (newlineIndex < 0) {
            return sanitized;
        }
        String fenceDescriptor = sanitized.substring(3, newlineIndex).trim();
        if (!fenceDescriptor.isEmpty() && !"markdown".equalsIgnoreCase(fenceDescriptor) && !"md".equalsIgnoreCase(fenceDescriptor)) {
            return sanitized;
        }
        String withoutOpeningFence = sanitized.substring(newlineIndex + 1);
        String trimmedWithoutOpening = org.springframework.util.StringUtils.trimWhitespace(withoutOpeningFence);
        if (trimmedWithoutOpening.endsWith("```")) {
            trimmedWithoutOpening = trimmedWithoutOpening.substring(0, trimmedWithoutOpening.length() - 3);
        }
        return org.springframework.util.StringUtils.trimWhitespace(trimmedWithoutOpening);
    }

    private boolean isValidFileUrl(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return false;
        }
        String trimmed = filePath.trim();
        if (trimmed.startsWith("/")) {
            return true;
        }
        try {
            URI uri = new URI(trimmed);
            return uri.getScheme() != null; // http, https, file, etc.
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String resolveFileName(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < normalized.length() - 1) {
            return normalized.substring(lastSlash + 1);
        }
        return normalized;
    }
}
