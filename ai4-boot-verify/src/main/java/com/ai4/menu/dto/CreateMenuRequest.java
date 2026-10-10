package com.ai4.menu.dto;

import jakarta.validation.constraints.*;

/**
 * 新增菜单请求
 */
public record CreateMenuRequest(
        @NotBlank(message = "菜单类型不能为空")
        @Pattern(
                regexp = "DIR|MENU|BUTTON",
                message = "菜单类型只能是DIR,MENU,BUTTON"
        )
        String menuType,

        @NotBlank(message = "菜单名称不能为空")
        @Size(max = 50, message = "菜单名称不能超过 50 个字符")
        String menuName,

        @NotNull(message = "父菜单 ID 不能为空")
        @Min(value = 0, message = "父菜单 ID 不能小于 0")
        Long parentId,

        @Size(max = 200, message = "路由地址不能超过 200 个字符")
        String path,

        @Size(max = 200, message = "组件路径不能超过 200 个字符")
        String component,

        @Min(value = 0, message = "显示顺序不能小于 0")
        Integer orderNum,

        @Size(max = 100, message = "权限标识不能超过 100 个字符")
        String perms,

        @Size(max = 100, message = "图标不能超过 100 个字符")
        String icon,

        @Min(value = 0, message = "状态只能是 0 或 1")
        @Max(value = 1, message = "状态只能是 0 或 1")
        Integer status
){
}
