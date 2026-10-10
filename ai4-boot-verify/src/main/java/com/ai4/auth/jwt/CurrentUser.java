package com.ai4.auth.jwt;

/**
 * 从合法 JWT 中解析出的当前用户身份。
 */
public record CurrentUser(
        long userId,
        String username
){
}
