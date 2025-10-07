package com.ai.middle.platform.service.impl;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.common.util.JwtUtil;
import com.ai.middle.platform.dto.request.LoginRequest;
import com.ai.middle.platform.dto.response.LoginResponse;
import com.ai.middle.platform.dto.response.UserDTO;
import com.ai.middle.platform.entity.po.SysUser;
import com.ai.middle.platform.repository.mapper.SysUserMapper;
import com.ai.middle.platform.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户信息
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, request.getEmail()));

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("用户已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 生成访问令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 构建用户信息
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();

        return LoginResponse.builder()
                .token(token)
                .user(userDTO)
                .build();
    }

    @Override
    public void logout(String token) {
        // 复杂场景可将Token加入黑名单或存储于Redis，这里交由客户端删除Token
    }
}
