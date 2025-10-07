package com.ai.middle.platform.controller;

import com.ai.middle.platform.common.constant.ApiConstants;
import com.ai.middle.platform.common.result.Result;
import com.ai.middle.platform.dto.request.KnowledgeBaseRequest;
import com.ai.middle.platform.dto.response.KnowledgeBaseDTO;
import com.ai.middle.platform.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping(ApiConstants.KB_PATH)
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 查询知识库列表
     *
     * @return 知识库列表
     */
    @GetMapping
    public Result<List<KnowledgeBaseDTO>> list() {
        List<KnowledgeBaseDTO> list = knowledgeBaseService.list();
        return Result.success(list);
    }

    /**
     * 查询知识库详情
     *
     * @param id 知识库ID
     * @return 知识库详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBaseDTO> getById(@PathVariable String id) {
        KnowledgeBaseDTO kb = knowledgeBaseService.getById(id);
        return Result.success(kb);
    }

    /**
     * 创建知识库
     *
     * @param request 创建请求参数
     * @return 创建后的知识库信息
     */
    @PostMapping
    public Result<KnowledgeBaseDTO> create(@Validated @RequestBody KnowledgeBaseRequest request) {
        KnowledgeBaseDTO kb = knowledgeBaseService.create(request);
        return Result.success(kb);
    }

    /**
     * 删除知识库
     *
     * @param id 知识库ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        knowledgeBaseService.delete(id);
        return Result.success();
    }
}
