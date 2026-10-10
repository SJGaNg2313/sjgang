package com.ai4.account.client.dto;

/**
 * 调用客户端用户服务重置密码时发送的数据。
 */
public record InternalResetPasswordRequest(
        String userId,
        String password
) {
}
