package com.ai4.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 读取 ai4.jwt 前缀下的 JWT 配置。
 */
@ConfigurationProperties(prefix = "ai4.jwt")
public record JwtProperties(
        String secret,
        long expireSeconds
){
}
