package com.ai4.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建用户请求对象
 *
 * 作用
 * 1.接收前端创建用户时传过来的json数据
 * 2.定义创建用户需要哪些字段
 * 3.配合@Valid触发参数校验
 */
public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        String user_name,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
