package com.ai4.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 用户查询结果，不包含密码
 */
public record UserListItemResponse(
        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("user_name")
        String userName,

        Integer status
){
}
