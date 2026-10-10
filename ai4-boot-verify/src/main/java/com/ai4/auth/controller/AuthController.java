package com.ai4.auth.controller;

import com.ai4.auth.dto.LoginRequest;
import com.ai4.auth.dto.LoginResponse;
import com.ai4.auth.dto.ProfileResponse;
import com.ai4.auth.dto.RouteResponse;
import com.ai4.auth.interceptor.JwtAuthenticationInterceptor;
import com.ai4.auth.jwt.CurrentUser;
import com.ai4.auth.repository.AuthPermissionRepository;
import com.ai4.auth.service.AuthRouteService;
import com.ai4.auth.service.AuthService;
import com.ai4.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 登录认证接口
 */
@RestController
public class AuthController {

    private final AuthService authService;
    private final AuthPermissionRepository authPermissionRepository;
    private final AuthRouteService authRouteService;

    public AuthController(
            AuthService authService,
            AuthPermissionRepository authPermissionRepository,
            AuthRouteService authRouteService
    ) {
        this.authService = authService;
        this.authPermissionRepository = authPermissionRepository;
        this.authRouteService = authRouteService;
    }

    /**
     * 校验账号密码并返回 JWT。
     */
    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * 返回拦截器已经认证的当前用户信息。
     */
    @GetMapping("/auth/profile")
    public ApiResponse<ProfileResponse> profile(
            @RequestAttribute(JwtAuthenticationInterceptor.CURRENT_USER_ATTRIBUTE) CurrentUser currentUser
    ) {
        ProfileResponse response = new ProfileResponse(
                currentUser.userId(),
                currentUser.username(),
                authPermissionRepository.findRoleNamesByUserId(currentUser.userId())
        );
        return ApiResponse.success(response);
    }

    /**
     * 返回当前登录用户的权限码
     */
    @GetMapping("/auth/perms")
    public ApiResponse<List<String>> permissions(
            @RequestAttribute(JwtAuthenticationInterceptor.CURRENT_USER_ATTRIBUTE)
            CurrentUser currentUser
    ) {
        List<String> permissions =
                authPermissionRepository.findPermissionCodesByUserId(currentUser.userId());

        return ApiResponse.success(permissions);
    }

    /**
     *返回当前登录用户可以访问的动态路由
     */
    @GetMapping("/auth/routes")
    public ApiResponse<List<RouteResponse>> routes(
            @RequestAttribute(JwtAuthenticationInterceptor.CURRENT_USER_ATTRIBUTE)
            CurrentUser currentUser
    ){
        List<RouteResponse> routes =
                authRouteService.findRoutesByUserId(currentUser.userId());

        return ApiResponse.success(routes);
    }
}
