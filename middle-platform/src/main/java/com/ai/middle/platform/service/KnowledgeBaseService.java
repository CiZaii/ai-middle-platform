package com.ai.middle.platform.service;

import com.ai.middle.platform.dto.request.KnowledgeBaseRequest;
import com.ai.middle.platform.dto.response.KnowledgeBaseDTO;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 查询知识库列表
     *
     * @return 知识库列表
     */
    List<KnowledgeBaseDTO> list();

    /**
     * 查询知识库详情
     *
     * @param id 知识库ID
     * @return 知识库详情
     */
    KnowledgeBaseDTO getById(String id);

    /**
     * 创建知识库
     *
     * @param request 创建请求参数
     * @return 创建后的知识库信息
     */
    KnowledgeBaseDTO create(KnowledgeBaseRequest request);

    /**
     * 删除知识库
     *
     * @param id 知识库ID
     */
    void delete(String id);
}
