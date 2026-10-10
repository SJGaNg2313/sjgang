package com.ai4.auth.interceptor;

import com.ai4.auth.jwt.CurrentUser;
import com.ai4.auth.jwt.JwtService;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在请求进入controller前校验JWT
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    /**
     * CurrentUser 保存在 request 中时使用的属性名
     */
    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final JwtService jwtService;

    public JwtAuthenticationInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * 提取并校验 Bearer token,通过后保存当前用户
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        String token = authorization
                .substring("Bearer ".length())
                .trim();

        if (token.isBlank()) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        CurrentUser currentUser = jwtService.parseToken(token);

        request.setAttribute(
                CURRENT_USER_ATTRIBUTE,
                currentUser
        );

        return true;
    }
}
