package com.ai4.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 客户端用户服务内部接口配置。
 */
@ConfigurationProperties(prefix = "ai4.client-user")
public record ClientUserProperties(
        String baseUrl,
        String internalToken,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
}
