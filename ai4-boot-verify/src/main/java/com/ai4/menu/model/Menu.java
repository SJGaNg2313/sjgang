package com.ai4.menu.model;

import java.time.LocalDateTime;

/**
 * 对应sys_menu的完整菜单数据
 */
public record Menu(
        Long menuId,
        String menuType,
        String menuName,
        Long parentId,
        String path,
        String component,
        Integer orderNum,
        String perms,
        String icon,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
}
