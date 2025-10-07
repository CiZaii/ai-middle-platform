package com.ai.middle.platform.repository.mapper;

import com.ai.middle.platform.entity.po.ModelBusiness;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for the model business configuration table.
 */
@Mapper
public interface ModelBusinessMapper extends BaseMapper<ModelBusiness> {
    // BaseMapper provides standard CRUD operations.
}
