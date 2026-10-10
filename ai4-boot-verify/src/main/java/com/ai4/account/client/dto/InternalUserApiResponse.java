package com.ai4.account.client.dto;

/**
 * 客户端用户服务的统一响应结构。
 */
public record InternalUserApiResponse<T>(
        Integer code,
        String msg,
        T data
) {
}
