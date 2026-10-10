package com.ai4.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台重置服务账户密码时接收的数据。
 */
public record ResetAccountPasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度必须在 6 到 64 之间")
        String newPassword
) {
}
