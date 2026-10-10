package com.ai4.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** 跨域访问配置。 */
@ConfigurationProperties(prefix = "ai4.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
