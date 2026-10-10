package com.ai4.auth.repository;

import com.ai4.auth.model.RouteMenu;

import java.util.List;

/**
 * 认证模块读取角色和权限的数据访问接口。
 */
public interface AuthPermissionRepository {

    /**
     * 根据用户 ID 查询当前用户拥有的角色名称。
     */
    List<String> findRoleNamesByUserId(long userId);

    /**
     * 根据用户 ID 查询当前用户拥有的权限码。
     */
    List<String> findPermissionCodesByUserId(long userId);

    /**
     * 根据用户 ID 查询当前用户可以访问的扁平菜单。
     */
    List<RouteMenu> findRoutesByUserId(long userId);
}
