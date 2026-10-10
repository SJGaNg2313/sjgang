package com.ai4.user.controller;


import com.ai4.common.response.ApiResponse;
import com.ai4.user.dto.*;
import com.ai4.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 创建用户接口
 * 作用：
 * 1.接收和用户相关的HTTP请求
 * 2.把请求数据交给后续业务层处理
 *
 */

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 创建用户接口
     * 请求方式：POST
     * 请求路路径：/users
     *
     */
    @PostMapping("/users")
    public ApiResponse<CreateUserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ){
        CreateUserResponse response = userService.createUser(request);
        return ApiResponse.success(response);
    }

    /**
     * 查询所有后台用户
     */
    @GetMapping("/users")
    public ApiResponse<List<UserListItemResponse>> listUsers(){
        return ApiResponse.success(userService.listUsers());
    }

    /**
     * 修改后台用户的状态
     */
    @PutMapping("/users/{userId}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable  Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
            ){
        userService.updateStatus(userId,request.status());
        return ApiResponse.success(null);
    }

    /**
     * 重置后台用户密码
     */
    @PutMapping("/users/{userId}/password")
    public ApiResponse<Void> resetUserPassword(
            @PathVariable Long userId,
            @Valid @RequestBody ResetUserPasswordRequest request
            ){
        userService.resetUserPassword(userId, request.password());
        return ApiResponse.success(null);
    }
}
