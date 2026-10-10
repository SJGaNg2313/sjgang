package com.ai4.menu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单树中的单个节点。
 */
public record MenuTreeResponse(
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

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt,

        List<MenuTreeResponse> children
) {
}
