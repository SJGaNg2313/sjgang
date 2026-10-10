package com.ai4.account.client.dto;

/**
 * 调用客户端用户服务创建用户时发送的数据。
 */
public record InternalCreateUserRequest(
        String phone,
        String password,
        String nick
) {
}
