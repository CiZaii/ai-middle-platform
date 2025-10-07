package com.ai.middle.platform.repository.mapper;

import com.ai.middle.platform.entity.po.KbProcessTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for the knowledge base process task table.
 */
@Mapper
public interface KbProcessTaskMapper extends BaseMapper<KbProcessTask> {
    // BaseMapper provides standard CRUD operations.
}
