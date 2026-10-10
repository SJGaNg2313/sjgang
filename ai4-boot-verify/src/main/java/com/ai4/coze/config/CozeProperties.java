package com.ai4.coze.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Coze OAuth JWT 接入配置。
 */
@Validated
@ConfigurationProperties(prefix = "ai4.coze")
public record CozeProperties(
        boolean enabled,
        @NotBlank String baseUrl,
        String oauthConfigPath,
        @Positive int timeoutSeconds
) {
}
