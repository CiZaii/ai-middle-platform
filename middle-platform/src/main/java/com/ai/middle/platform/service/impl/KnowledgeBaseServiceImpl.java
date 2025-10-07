package com.ai.middle.platform.service.impl;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.common.util.IdUtil;
import com.ai.middle.platform.dto.request.KnowledgeBaseRequest;
import com.ai.middle.platform.dto.response.KnowledgeBaseDTO;
import com.ai.middle.platform.dto.response.MemberDTO;
import com.ai.middle.platform.dto.response.UserDTO;
import com.ai.middle.platform.entity.po.KbKnowledgeBase;
import com.ai.middle.platform.entity.po.KbMember;
import com.ai.middle.platform.entity.po.SysUser;
import com.ai.middle.platform.repository.mapper.KbKnowledgeBaseMapper;
import com.ai.middle.platform.repository.mapper.KbMemberMapper;
import com.ai.middle.platform.repository.mapper.SysUserMapper;
import com.ai.middle.platform.service.KnowledgeBaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KbKnowledgeBaseMapper kbMapper;
    private final KbMemberMapper memberMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<KnowledgeBaseDTO> list() {
        List<KbKnowledgeBase> kbList = kbMapper.selectList(null);
        return kbList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public KnowledgeBaseDTO getById(String id) {
        KbKnowledgeBase kb = kbMapper.selectOne(new LambdaQueryWrapper<KbKnowledgeBase>()
                .eq(KbKnowledgeBase::getKbId, id));

        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }

        return convertToDTO(kb);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseDTO create(KnowledgeBaseRequest request) {
        // TODO: 从安全上下文获取当前用户ID
        Long currentUserId = 1L;

        // 创建知识库记录
        KbKnowledgeBase kb = KbKnowledgeBase.builder()
                .kbId(IdUtil.simpleUUID())
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(currentUserId)
                .fileCount(0)
                .enabled(true)
                .build();

        kbMapper.insert(kb);

        return convertToDTO(kb);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        KbKnowledgeBase kb = kbMapper.selectOne(new LambdaQueryWrapper<KbKnowledgeBase>()
                .eq(KbKnowledgeBase::getKbId, id));

        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }

        // 级联删除由数据库约束处理
        kbMapper.deleteById(kb.getId());
    }

    /**
     * 将实体转换为DTO
     */
    private KnowledgeBaseDTO convertToDTO(KbKnowledgeBase kb) {
        SysUser owner = userMapper.selectById(kb.getOwnerId());
        if (owner == null) {
            throw new BusinessException("知识库拥有者不存在");
        }

        UserDTO ownerDTO = UserDTO.builder()
                .id(owner.getId())
                .username(owner.getUsername())
                .email(owner.getEmail())
                .name(owner.getName())
                .avatar(owner.getAvatar())
                .role(owner.getRole())
                .build();

        List<KbMember> members = memberMapper.selectList(new LambdaQueryWrapper<KbMember>()
                .eq(KbMember::getKbId, kb.getId()));

        List<MemberDTO> memberDTOs = members.stream()
                .map(member -> {
                    SysUser user = userMapper.selectById(member.getUserId());
                    if (user == null) {
                        throw new BusinessException("成员用户不存在");
                    }

                    UserDTO userDTO = UserDTO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .name(user.getName())
                            .avatar(user.getAvatar())
                            .role(user.getRole())
                            .build();

                    return MemberDTO.builder()
                            .user(userDTO)
                            .role(member.getRole())
                            .joinedAt(member.getJoinedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return KnowledgeBaseDTO.builder()
                .id(kb.getKbId())
                .name(kb.getName())
                .description(kb.getDescription())
                .fileCount(kb.getFileCount())
                .owner(ownerDTO)
                .members(memberDTOs)
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }
}
