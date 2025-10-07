package com.ai.middle.platform.service.impl;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.common.util.IdGenerator;
import com.ai.middle.platform.dto.request.PromptRequest;
import com.ai.middle.platform.dto.response.PromptDTO;
import com.ai.middle.platform.entity.po.KbPrompt;
import com.ai.middle.platform.repository.mapper.KbPromptMapper;
import com.ai.middle.platform.service.PromptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Prompt 管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    private final KbPromptMapper promptMapper;
    private final ObjectMapper objectMapper;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    @Override
    public List<PromptDTO> listAll() {
        List<KbPrompt> prompts = promptMapper.selectList(new LambdaQueryWrapper<KbPrompt>()
                .orderByDesc(KbPrompt::getCreatedAt));
        return prompts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PromptDTO getById(Long id) {
        KbPrompt prompt = promptMapper.selectById(id);
        if (prompt == null) {
            throw new BusinessException("Prompt 不存在");
        }
        return convertToDTO(prompt);
    }

    @Override
    @Cacheable(value = "prompt", key = "#businessCode")
    public PromptDTO getByBusinessCode(String businessCode) {
        KbPrompt prompt = promptMapper.selectOne(new LambdaQueryWrapper<KbPrompt>()
                .eq(KbPrompt::getBusinessCode, businessCode)
                .eq(KbPrompt::getIsActive, true));
        
        if (prompt == null) {
            log.warn("未找到业务代码对应的激活 Prompt: {}", businessCode);
            throw new BusinessException("未配置该业务的 Prompt: " + businessCode);
        }
        
        return convertToDTO(prompt);
    }

    @Override
    @Cacheable(value = "promptContent", key = "#businessCode")
    public String getActivePromptContent(String businessCode) {
        PromptDTO prompt = getByBusinessCode(businessCode);
        return prompt.getPromptContent();
    }

    @Override
    public String formatPrompt(String promptTemplate, Map<String, Object> variables) {
        if (!StringUtils.hasText(promptTemplate)) {
            return promptTemplate;
        }

        if (variables == null || variables.isEmpty()) {
            return promptTemplate;
        }

        String result = promptTemplate;
        Matcher matcher = VARIABLE_PATTERN.matcher(promptTemplate);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            
            if (value != null) {
                String replacement = value.toString();
                // 使用 Matcher.quoteReplacement 避免特殊字符问题
                result = result.replace("{" + variableName + "}", Matcher.quoteReplacement(replacement));
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"prompt", "promptContent"}, allEntries = true)
    public PromptDTO create(PromptRequest request) {
        // 检查业务代码是否已存在
        KbPrompt existing = promptMapper.selectOne(new LambdaQueryWrapper<KbPrompt>()
                .eq(KbPrompt::getBusinessCode, request.getBusinessCode()));
        
        if (existing != null) {
            throw new BusinessException("该业务代码已存在: " + request.getBusinessCode());
        }

        KbPrompt prompt = KbPrompt.builder()
                .promptId(IdGenerator.simpleUUID())
                .businessCode(request.getBusinessCode())
                .promptName(request.getPromptName())
                .promptContent(request.getPromptContent())
                .description(request.getDescription())
                .variables(request.getVariables())
                .isActive(request.getIsActive())
                .version(1)
                .build();

        promptMapper.insert(prompt);
        log.info("创建 Prompt: businessCode={}, name={}", request.getBusinessCode(), request.getPromptName());
        
        return convertToDTO(prompt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"prompt", "promptContent"}, allEntries = true)
    public PromptDTO update(Long id, PromptRequest request) {
        KbPrompt prompt = promptMapper.selectById(id);
        if (prompt == null) {
            throw new BusinessException("Prompt 不存在");
        }

        // 如果修改了业务代码，检查新代码是否已被使用
        if (!prompt.getBusinessCode().equals(request.getBusinessCode())) {
            KbPrompt existing = promptMapper.selectOne(new LambdaQueryWrapper<KbPrompt>()
                    .eq(KbPrompt::getBusinessCode, request.getBusinessCode())
                    .ne(KbPrompt::getId, id));
            
            if (existing != null) {
                throw new BusinessException("该业务代码已被使用: " + request.getBusinessCode());
            }
        }

        prompt.setBusinessCode(request.getBusinessCode());
        prompt.setPromptName(request.getPromptName());
        prompt.setPromptContent(request.getPromptContent());
        prompt.setDescription(request.getDescription());
        prompt.setVariables(request.getVariables());
        prompt.setIsActive(request.getIsActive());
        prompt.setVersion(prompt.getVersion() + 1);

        promptMapper.updateById(prompt);
        log.info("更新 Prompt: id={}, businessCode={}", id, request.getBusinessCode());
        
        return convertToDTO(prompt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"prompt", "promptContent"}, allEntries = true)
    public void delete(Long id) {
        KbPrompt prompt = promptMapper.selectById(id);
        if (prompt == null) {
            throw new BusinessException("Prompt 不存在");
        }

        promptMapper.deleteById(id);
        log.info("删除 Prompt: id={}, businessCode={}", id, prompt.getBusinessCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"prompt", "promptContent"}, allEntries = true)
    public void toggleActive(Long id, Boolean isActive) {
        KbPrompt prompt = promptMapper.selectById(id);
        if (prompt == null) {
            throw new BusinessException("Prompt 不存在");
        }

        prompt.setIsActive(isActive);
        promptMapper.updateById(prompt);
        log.info("切换 Prompt 激活状态: id={}, businessCode={}, isActive={}", 
                id, prompt.getBusinessCode(), isActive);
    }

    private PromptDTO convertToDTO(KbPrompt prompt) {
        List<String> variables = parseVariables(prompt.getVariables());
        
        return PromptDTO.builder()
                .id(prompt.getId())
                .promptId(prompt.getPromptId())
                .businessCode(prompt.getBusinessCode())
                .promptName(prompt.getPromptName())
                .promptContent(prompt.getPromptContent())
                .description(prompt.getDescription())
                .variables(variables)
                .isActive(prompt.getIsActive())
                .version(prompt.getVersion())
                .createdAt(prompt.getCreatedAt())
                .updatedAt(prompt.getUpdatedAt())
                .build();
    }

    private List<String> parseVariables(String variablesJson) {
        if (!StringUtils.hasText(variablesJson)) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(variablesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 Prompt 变量失败: {}", variablesJson, e);
            return Collections.emptyList();
        }
    }
}
