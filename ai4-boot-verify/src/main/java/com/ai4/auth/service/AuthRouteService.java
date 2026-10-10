package com.ai4.auth.service;

import com.ai4.auth.dto.RouteResponse;
import com.ai4.auth.model.RouteMenu;
import com.ai4.auth.repository.AuthPermissionRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 负责查询并组装当前用户的动态路由。
 */
@Service
public class AuthRouteService {

    private final AuthPermissionRepository authPermissionRepository;

    public AuthRouteService(
            AuthPermissionRepository authPermissionRepository
    ) {
        this.authPermissionRepository = authPermissionRepository;
    }

    /**
     * 查询当前用户可访问的菜单，并组装为树形结构。
     */
    public List<RouteResponse> findRoutesByUserId(long userId) {
        List<RouteMenu> menus =
                authPermissionRepository.findRoutesByUserId(userId);

        return buildChildren(menus, 0L);
    }

    /**
     * 查找指定父菜单下的子菜单。
     */
    private List<RouteResponse> buildChildren(
            List<RouteMenu> menus,
            long parentId
    ) {
        return menus.stream()
                .filter(menu -> menu.parentId() == parentId)
                .sorted(
                        Comparator.comparingInt(RouteMenu::orderNum)
                                .thenComparingLong(RouteMenu::menuId)
                )
                .map(menu -> new RouteResponse(
                        menu.menuId(),
                        menu.menuType(),
                        menu.menuName(),
                        menu.parentId(),
                        menu.path(),
                        menu.component(),
                        menu.icon(),
                        menu.orderNum(),
                        buildChildren(menus, menu.menuId())
                ))
                .toList();
    }
}
