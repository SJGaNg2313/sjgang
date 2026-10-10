package com.ai4.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置后台用户密码
 */
public record ResetUserPasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max= 64, message = "密码长度在6-64位之间")
        String password
){
}
