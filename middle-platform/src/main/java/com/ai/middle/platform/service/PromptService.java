package com.ai.middle.platform.service;

import com.ai.middle.platform.dto.request.PromptRequest;
import com.ai.middle.platform.dto.response.PromptDTO;

import java.util.List;
import java.util.Map;

/**
 * Prompt 管理服务接口
 */
public interface PromptService {

    /**
     * 获取所有 Prompt 列表
     */
    List<PromptDTO> listAll();

    /**
     * 根据 ID 获取 Prompt
     */
    PromptDTO getById(Long id);

    /**
     * 根据业务代码获取 Prompt
     */
    PromptDTO getByBusinessCode(String businessCode);

    /**
     * 获取激活的 Prompt 内容
     */
    String getActivePromptContent(String businessCode);

    /**
     * 格式化 Prompt（替换变量）
     */
    String formatPrompt(String prompt, Map<String, Object> variables);

    /**
     * 创建 Prompt
     */
    PromptDTO create(PromptRequest request);

    /**
     * 更新 Prompt
     */
    PromptDTO update(Long id, PromptRequest request);

    /**
     * 删除 Prompt
     */
    void delete(Long id);

    /**
     * 激活/停用 Prompt
     */
    void toggleActive(Long id, Boolean isActive);
}
