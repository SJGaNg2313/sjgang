package com.ai4.role.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.role.dto.AssignRoleMenuRequest;
import com.ai4.role.service.RoleMenuService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色菜单授权接口
 */
@RestController
@RequestMapping("/roles")
public class RoleMenuController {
    private final RoleMenuService roleMenuService;

    public RoleMenuController(RoleMenuService roleMenuService){
        this.roleMenuService = roleMenuService;
    }

    /**
     * 查询角色当前关联的菜单id
     */
    @GetMapping("/{roleId}/menus")
    public ApiResponse<List<Long>> findMenuIds(@PathVariable long roleId){
        return ApiResponse.success(roleMenuService.findMenuIds(roleId));
    }

    /**
     * 替换角色菜单授权
     */
    @PutMapping("/{roleId}/menus")
    public ApiResponse<List<Long>> replaceMenus(
            @PathVariable long roleId,
            @Valid @RequestBody AssignRoleMenuRequest request
            ){
        return ApiResponse.success(
                roleMenuService.replaceMenus(roleId,request.menuIds())
        );
    }
}
