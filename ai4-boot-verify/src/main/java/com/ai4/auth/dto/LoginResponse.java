package com.ai4.auth.dto;

/**
 * 登录成功后返回的 JWT 信息。
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expireIn
) {
}
