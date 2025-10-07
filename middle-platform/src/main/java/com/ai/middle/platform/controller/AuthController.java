package com.ai.middle.platform.controller;

import com.ai.middle.platform.common.constant.ApiConstants;
import com.ai.middle.platform.common.result.Result;
import com.ai.middle.platform.dto.request.LoginRequest;
import com.ai.middle.platform.dto.response.LoginResponse;
import com.ai.middle.platform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping(ApiConstants.AUTH_PATH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 用户登出
     *
     * @param token 待注销的Token
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(ApiConstants.JWT_HEADER) String token) {
        authService.logout(token);
        return Result.success();
    }
}
