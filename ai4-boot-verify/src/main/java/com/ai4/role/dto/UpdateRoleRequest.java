package com.ai4.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称不能超过50个字符")
        String roleName,

        Integer roleSort
){
}
