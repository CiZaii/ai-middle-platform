package com.ai.middle.platform.repository.mapper;

import com.ai.middle.platform.entity.po.KbKnowledgeBase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for the knowledge base table.
 */
@Mapper
public interface KbKnowledgeBaseMapper extends BaseMapper<KbKnowledgeBase> {
    // BaseMapper provides standard CRUD operations.
}
