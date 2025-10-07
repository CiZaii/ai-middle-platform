package com.ai.middle.platform.repository.mapper;

import com.ai.middle.platform.entity.po.ModelInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for the model info table.
 */
@Mapper
public interface ModelInfoMapper extends BaseMapper<ModelInfo> {
    // BaseMapper provides standard CRUD operations.
}
