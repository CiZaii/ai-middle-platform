package com.ai.middle.platform.repository.mapper;

import com.ai.middle.platform.entity.po.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for the user table.
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // BaseMapper provides standard CRUD operations.
}
