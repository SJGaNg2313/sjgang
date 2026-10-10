package com.ai4.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignUserRolesRequest(
        @NotNull(message = "角色 ID 列表不能为空")
        List<@NotNull(message = "角色 ID 不能为空") Long> roleIds
) {
}
