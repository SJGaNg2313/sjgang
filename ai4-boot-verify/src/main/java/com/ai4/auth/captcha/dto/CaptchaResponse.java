package com.ai4.auth.captcha.dto;

/** 本地图形验证码响应。 */
public record CaptchaResponse(
        String captchaId,
        String image
) {
}
