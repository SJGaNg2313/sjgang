package com.ai4.account.dto;

/**
 * 创建服务账户成功后返回的非敏感字段。
 */
public record CreateAccountResponse(
        String userId,
        String account,
        String nick,
        Integer status
) {
}
