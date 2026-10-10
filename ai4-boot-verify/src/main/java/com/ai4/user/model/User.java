package com.ai4.user.model;

/**
 * 用户数据对象，对应 users 表中的核心字段。
 */
public record User(
        Long userId,
        String userName,
        String password,
        Integer status
) {
}
