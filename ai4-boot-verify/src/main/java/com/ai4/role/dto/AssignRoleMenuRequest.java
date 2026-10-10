package com.ai4.role.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignRoleMenuRequest (
        @NotNull (message = "菜单id列表不能为空")
        List<@NotNull(message = "菜单ID不能为空") Long> menuIds
){
}
