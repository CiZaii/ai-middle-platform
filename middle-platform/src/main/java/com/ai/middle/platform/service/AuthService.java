package com.ai.middle.platform.service;

import com.ai.middle.platform.dto.request.LoginRequest;
import com.ai.middle.platform.dto.response.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 登录结果
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出
     *
     * @param token 待注销的访问令牌
     */
    void logout(String token);
}
