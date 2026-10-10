package com.ai4.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求对象
 * 作用：
 * 1.接收前端登录时提交的username和password
 * 2.配合@Valid做参数校验
 */
public record LoginRequest (
        @NotBlank(message = "用户名不能为空")
        String user_name,

        @NotBlank(message = "密码不能为空")
        String password,

        String captcha_id,

        String captcha_code
){
}
