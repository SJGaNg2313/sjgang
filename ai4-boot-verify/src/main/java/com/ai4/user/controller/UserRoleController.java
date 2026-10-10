package com.ai4.user.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.user.dto.AssignUserRolesRequest;
import com.ai4.user.service.UserRoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户角色授权接口。
 */
@RestController
@RequestMapping("/users")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    /**
     * 查询用户当前关联的角色 ID
     */
    @GetMapping("/{userId}/roles")
    public ApiResponse<List<Long>> findRoleIds(
            @PathVariable long userId
    ) {
        return ApiResponse.success(userRoleService.findRoleIds(userId));
    }

    /**
     * 替换用户角色
     */
    @PutMapping("/{userId}/roles")
    public ApiResponse<List<Long>> replaceRoles(
            @PathVariable long userId,
            @Valid @RequestBody AssignUserRolesRequest request
    ) {
        return ApiResponse.success(
                userRoleService.replaceRoles(userId, request.roleIds())
        );
    }
}
