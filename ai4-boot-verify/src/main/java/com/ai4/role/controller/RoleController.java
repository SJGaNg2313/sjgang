package com.ai4.role.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.role.dto.CreatedRoleRequest;
import com.ai4.role.dto.UpdateRoleRequest;
import com.ai4.role.dto.UpdateRoleStatusRequest;
import com.ai4.role.model.Role;
import com.ai4.role.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService){
        this.roleService = roleService;
    }

    /**
     * 查询全部角色
     */
    @GetMapping
    public ApiResponse<List<Role>> list(){
        return ApiResponse.success(roleService.list());
    }

    /**
     * 查询角色详情
     */
    @GetMapping("/{roleId}")
    public ApiResponse<Role> get(@PathVariable long roleId){
        return ApiResponse.success(roleService.get(roleId));
    }

    /**
     * 创建角色
     */
    @PostMapping
    public ApiResponse<Role> create(
            @Valid @RequestBody CreatedRoleRequest request
            ){
        return ApiResponse.success(roleService.create(request));
    }

    /**
     * 修改角色名称和显示顺序
     */
    @PutMapping("/{roleId}")
    public ApiResponse<Role> update(
            @PathVariable long roleId,
            @Valid @RequestBody UpdateRoleRequest request
    ){
        return ApiResponse.success(roleService.update(roleId,request));
    }

    /**
     * 修改角色状态
     */
    @PutMapping("/{roleId}/status")
    public ApiResponse<Role> updateStatus(
            @PathVariable long roleId,
            @Valid @RequestBody UpdateRoleStatusRequest request
    ){
        return ApiResponse.success(
                roleService.updateStatus(roleId,request.status())
        );
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{roleId}")
    public ApiResponse<Void> delete(
            @PathVariable long roleId
    ){
        roleService.delete(roleId);
        return ApiResponse.success(null);
    }
}
