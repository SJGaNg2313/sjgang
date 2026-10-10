package com.ai4.role.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleStatusRequest (
        @NotNull(message = "角色状态不能为空")
        Integer status
){
}
