package com.ai.middle.platform.repository.mapper;

import com.ai.middle.platform.entity.po.KbMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for the knowledge base member table.
 */
@Mapper
public interface KbMemberMapper extends BaseMapper<KbMember> {
    // BaseMapper provides standard CRUD operations.
}
