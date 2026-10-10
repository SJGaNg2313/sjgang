package com.ai4.auth.model;

/**
 * 从数据库查询出的扁平菜单数据。
 */
public record RouteMenu(
        long menuId,
        String menuType,
        String menuName,
        long parentId,
        String path,
        String component,
        String icon,
        int orderNum
) {
}

