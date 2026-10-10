package com.ai4.auth.interceptor;

import com.ai4.auth.annotation.RequirePermission;
import com.ai4.auth.jwt.CurrentUser;
import com.ai4.auth.repository.AuthPermissionRepository;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 根据 Controller 方法声明的权限码校验当前用户权限。
 */
@Component
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {

    private final AuthPermissionRepository authPermissionRepository;

    public PermissionAuthorizationInterceptor(AuthPermissionRepository authPermissionRepository) {
        this.authPermissionRepository = authPermissionRepository;
    }

    /**
     * Controller 方法声明权限时，检查当前用户是否拥有该权限。
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            return true;
        }

        CurrentUser currentUser = (CurrentUser) request.getAttribute(
                JwtAuthenticationInterceptor.CURRENT_USER_ATTRIBUTE
        );
        if (currentUser == null) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        List<String> permissions = authPermissionRepository
                .findPermissionCodesByUserId(currentUser.userId());
        if (!permissions.contains(annotation.value())) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        return true;
    }
}
