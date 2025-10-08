package com.ai.middle.platform.service.processing.impl;

import com.ai.middle.platform.service.PromptService;
import com.ai.middle.platform.service.processing.ChatExecutor;
import com.ai.middle.platform.service.processing.TagProcessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagProcessorImpl implements TagProcessor {

    private static final String BUSINESS_TAG = "tag";
    private static final int TAG_CONTENT_PREVIEW_LENGTH = 500;
    private static final int TAG_COUNT = 5;

    private final PromptService promptService;
    private final ChatExecutor chatExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> generate(String fileId, String fileName, String content) {
        log.info("========================================");
        log.info("开始标签生成任务");
        log.info("文件ID: {}", fileId);
        log.info("文件名称: {}", fileName);
        log.info("内容长度: {} 字符", content != null ? content.length() : 0);
        log.info("========================================");

        if (!StringUtils.hasText(content)) {
            log.warn("内容为空，无法生成标签: fileId={}", fileId);
            return List.of();
        }

        String contentPreview = content.length() > TAG_CONTENT_PREVIEW_LENGTH
                ? content.substring(0, TAG_CONTENT_PREVIEW_LENGTH)
                : content;
        String promptTemplate = promptService.getActivePromptContent(BUSINESS_TAG);
        Map<String, Object> variables = new HashMap<>();
        variables.put("tagCount", TAG_COUNT);
        variables.put("fileName", fileName);
        variables.put("content", contentPreview);
        String prompt = promptService.formatPrompt(promptTemplate, variables);

        try {
            String response = chatExecutor.execute(BUSINESS_TAG, (chatModel, runtimeConfig) -> chatModel.generate(prompt));
            return parseTags(response, fileId);
        } catch (Exception e) {
            log.error("标签生成失败: fileId={}, error={}", fileId, e.getMessage(), e);
            return List.of();
        }
    }

    private List<String> parseTags(String response, String fileId) {
        String json = extractJson(response);
        if (!StringUtils.hasText(json)) {
            log.warn("标签响应为空或格式不正确: fileId={}", fileId);
            return List.of();
        }
        try {
            List<String> tags = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return tags.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .filter(tag -> tag.length() >= 2 && tag.length() <= 20)
                    .limit(TAG_COUNT)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("解析标签结果失败: fileId={} response={}", fileId, response, e);
            return List.of();
        }
    }

    private String extractJson(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int closingFence = trimmed.lastIndexOf("```");
            if (closingFence > -1) {
                trimmed = trimmed.substring(0, closingFence);
            }
        }
        return trimmed.trim();
    }
}

