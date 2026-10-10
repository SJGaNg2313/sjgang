package com.ai4.user.dto;

/**
* 创建用户对象
 * 作用：
 * 1.定义创建用户成功后返回给前端的数据
 * 2.和CreateUserRequest分开，避免把password返回给前端
 */
public record CreateUserResponse(Long user_id,String user_name) {
}
