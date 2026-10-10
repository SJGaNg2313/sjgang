package com.ai4.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建学员账户时接收的数据。
 */
public record CreateAccountRequest(
        @NotBlank(message = "手机号不能为空")
        @Size(max = 20, message = "手机号长度不能超过 20")
        String phone,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度必须在 6 到 64 之间")
        String password,

        @Size(max = 255, message = "昵称长度不能超过 255")
        String nick
) {
}
