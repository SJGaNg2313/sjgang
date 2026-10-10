package com.ai4.auth.dto;

import java.util.List;

/**
 * 当前用户可以访问的菜单路由。
 */
public record RouteResponse(
        long menuId,
        String menuType,
        String menuName,
        long parentId,
        String path,
        String component,
        String icon,
        int orderNum,
        List<RouteResponse> children
){
}
