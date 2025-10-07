package com.ai.middle.platform.repository.mapper;

import com.ai.middle.platform.entity.po.ModelEndpointBusiness;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for the model endpoint-business relation table.
 */
@Mapper
public interface ModelEndpointBusinessMapper extends BaseMapper<ModelEndpointBusiness> {
    // BaseMapper provides standard CRUD operations.
}
