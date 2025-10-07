package com.ai.middle.platform.repository.mapper;

import com.ai.middle.platform.entity.po.ModelEndpoint;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for the model endpoint configuration table.
 */
@Mapper
public interface ModelEndpointMapper extends BaseMapper<ModelEndpoint> {
    // BaseMapper provides standard CRUD operations.
}
