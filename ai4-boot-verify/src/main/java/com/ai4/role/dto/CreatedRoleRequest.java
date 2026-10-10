package com.ai4.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatedRoleRequest (
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称不能超过50个字符")
        String roleName,

        @NotBlank(message = "角色编码不能为空")
        @Size(max = 50, message = "角色编码不能超过50个字符")
        @Pattern(
                regexp = "^[a-z][a-z0-9_]*$",
                message = "角色编码只能使用小写字母、数字和下划线，并且必须以字母开头"
        )
        String roleCode,

        Integer status,

        Integer roleSort
){
}
