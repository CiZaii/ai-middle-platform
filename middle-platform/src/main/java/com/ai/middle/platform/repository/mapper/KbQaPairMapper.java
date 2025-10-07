package com.ai.middle.platform.repository.mapper;

import com.ai.middle.platform.entity.po.KbQaPair;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for the knowledge base QA pair table.
 */
@Mapper
public interface KbQaPairMapper extends BaseMapper<KbQaPair> {
    // BaseMapper provides standard CRUD operations.
}
