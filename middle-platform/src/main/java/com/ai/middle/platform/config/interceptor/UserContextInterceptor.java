package com.ai.middle.platform.config.interceptor;

import com.ai.middle.platform.common.constant.ApiConstants;
import com.ai.middle.platform.common.context.CurrentUser;
import com.ai.middle.platform.common.context.UserContextHolder;
import com.ai.middle.platform.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that resolves the authenticated user from the JWT token and stores it in a
 * {@link UserContextHolder} for downstream usage (e.g. auditing createdBy fields).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader(ApiConstants.JWT_HEADER);
        if (!StringUtils.hasText(authorization)) {
            UserContextHolder.clear();
            return true;
        }

        String token = authorization;
        if (authorization.startsWith(ApiConstants.JWT_PREFIX)) {
            token = authorization.substring(ApiConstants.JWT_PREFIX.length()).trim();
        }

        if (!StringUtils.hasText(token)) {
            UserContextHolder.clear();
            return true;
        }

        try {
            if (!jwtUtil.validateToken(token)) {
                UserContextHolder.clear();
                return true;
            }
            Claims claims = jwtUtil.getClaimsFromToken(token);
            Number userIdNumber = claims.get("userId", Number.class);
            if (userIdNumber == null) {
                UserContextHolder.clear();
                return true;
            }
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            CurrentUser currentUser = new CurrentUser(userIdNumber.longValue(), username, role);
            UserContextHolder.set(currentUser);
        } catch (Exception ex) {
            log.warn("Failed to resolve user context from token", ex);
            UserContextHolder.clear();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
