package com.ai4.account.client.dto;

/**
 * 客户端用户服务创建用户后返回的数据。
 */
public record InternalCreateUserResponse(
        String userId,
        String account,
        String nick,
        String phone
) {
}
