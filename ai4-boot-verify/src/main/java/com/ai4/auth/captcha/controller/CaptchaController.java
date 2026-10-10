package com.ai4.auth.captcha.controller;

import com.ai4.auth.captcha.dto.CaptchaResponse;
import com.ai4.auth.captcha.service.CaptchaService;
import com.ai4.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 本地图形验证码接口。 */
@RestController
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /** 获取验证码图片和对应的临时标识。 */
    @GetMapping("/auth/captcha")
    public ApiResponse<CaptchaResponse> generate() {
        return ApiResponse.success(captchaService.generate());
    }
}
