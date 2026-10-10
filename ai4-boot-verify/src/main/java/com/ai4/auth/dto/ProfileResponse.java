package com.ai4.auth.dto;

import java.util.List;

/**
 * 当前登录用户的基本信息和角色。
 */
public record ProfileResponse(
        long userId,
        String userName,
        List<String> roles
){
}
