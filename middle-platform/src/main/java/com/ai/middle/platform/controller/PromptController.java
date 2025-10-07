package com.ai.middle.platform.controller;

import com.ai.middle.platform.common.constant.ApiConstants;
import com.ai.middle.platform.common.result.Result;
import com.ai.middle.platform.dto.request.PromptRequest;
import com.ai.middle.platform.dto.response.PromptDTO;
import com.ai.middle.platform.service.PromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prompt 管理控制器
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX + "/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    /**
     * 获取所有 Prompt 列表
     */
    @GetMapping
    public Result<List<PromptDTO>> listAll() {
        List<PromptDTO> prompts = promptService.listAll();
        return Result.success(prompts);
    }

    /**
     * 根据 ID 获取 Prompt
     */
    @GetMapping("/{id}")
    public Result<PromptDTO> getById(@PathVariable Long id) {
        PromptDTO prompt = promptService.getById(id);
        return Result.success(prompt);
    }

    /**
     * 根据业务代码获取 Prompt
     */
    @GetMapping("/business/{businessCode}")
    public Result<PromptDTO> getByBusinessCode(@PathVariable String businessCode) {
        PromptDTO prompt = promptService.getByBusinessCode(businessCode);
        return Result.success(prompt);
    }

    /**
     * 创建 Prompt
     */
    @PostMapping
    public Result<PromptDTO> create(@Valid @RequestBody PromptRequest request) {
        PromptDTO prompt = promptService.create(request);
        return Result.success(prompt);
    }

    /**
     * 更新 Prompt
     */
    @PutMapping("/{id}")
    public Result<PromptDTO> update(@PathVariable Long id, @Valid @RequestBody PromptRequest request) {
        PromptDTO prompt = promptService.update(id, request);
        return Result.success(prompt);
    }

    /**
     * 删除 Prompt
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        promptService.delete(id);
        return Result.success();
    }

    /**
     * 激活/停用 Prompt
     */
    @PostMapping("/{id}/toggle")
    public Result<Void> toggleActive(@PathVariable Long id, @RequestParam Boolean isActive) {
        promptService.toggleActive(id, isActive);
        return Result.success();
    }
}
