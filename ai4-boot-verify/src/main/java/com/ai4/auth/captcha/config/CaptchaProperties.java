package com.ai4.auth.captcha.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 本地图形验证码配置。 */
@ConfigurationProperties(prefix = "ai4.auth.captcha")
public record CaptchaProperties(
        boolean enabled,
        int expireSeconds
) {
}
