package com.ai4.menu.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.menu.dto.CreateMenuRequest;
import com.ai4.menu.dto.MenuTreeResponse;
import com.ai4.menu.dto.UpdateMenuRequest;
import com.ai4.menu.model.Menu;
import com.ai4.menu.repository.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 菜单管理业务
 */
@Service
public class MenuService {

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository){
        this.menuRepository = menuRepository;
    }

    /**
     * 根据菜单ID查询菜单详情
     */
    public Menu get(Long menuId){
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new BizException(ErrorCode.MENU_NOT_FOUND));
    }

    /**
     * 查询全部菜单
     */
    public List<MenuTreeResponse> listTree(){
        List<Menu> menus = menuRepository.findAll();
        return buildChildren(menus,0L);
    }

    /**
     * 新增菜单。
     */
    @Transactional
    public Menu create(CreateMenuRequest request) {
        validateParent(request.parentId());

        validateFields(
                request.menuType(),
                request.path(),
                request.component(),
                request.perms()
        );

        Menu menu = new Menu(
                null,
                request.menuType(),
                request.menuName().trim(),
                request.parentId(),
                trimToNull(request.path()),
                trimToNull(request.component()),
                request.orderNum() == null ? 0 : request.orderNum(),
                trimToNull(request.perms()),
                trimToNull(request.icon()),
                request.status() == null ? 0 : request.status(),
                null,
                null
        );

        Menu savedMenu = menuRepository.save(menu);
        menuRepository.assignToSuperAdmin(savedMenu.menuId());
        return savedMenu;
    }

    /**
     * 修改菜单，并校验父菜单关系不能形成循环。
     */
    @Transactional
    public Menu update(Long menuId, UpdateMenuRequest request) {
        Menu existingMenu = get(menuId);
        validateParentForUpdate(menuId, request.parentId());
        validateFields(request.menuType(), request.path(), request.component(), request.perms());

        String normalizedPerms = trimToNull(request.perms());
        if (!Objects.equals(existingMenu.perms(), normalizedPerms)
                && menuRepository.countRoleRelations(menuId) > 0) {
            throw new BizException(ErrorCode.MENU_ASSIGNED_TO_ROLE);
        }

        Menu menu = new Menu(
                menuId, request.menuType(), request.menuName().trim(), request.parentId(),
                trimToNull(request.path()), trimToNull(request.component()),
                request.orderNum() == null ? existingMenu.orderNum() : request.orderNum(),
                normalizedPerms, trimToNull(request.icon()), request.status(),
                existingMenu.createdAt(), existingMenu.updatedAt()
        );

        if (menuRepository.update(menu) == 0) {
            throw new BizException(ErrorCode.MENU_NOT_FOUND);
        }
        return get(menuId);
    }

    /**
     * 删除没有子菜单且未分配给任何角色的菜单。
     */
    @Transactional
    public void delete(Long menuId) {
        get(menuId);
        if (menuRepository.countByParentId(menuId) > 0) {
            throw new BizException(ErrorCode.MENU_HAS_CHILDREN);
        }
        if (menuRepository.countRoleRelations(menuId) > 0) {
            throw new BizException(ErrorCode.MENU_ASSIGNED_TO_ROLE);
        }
        if (menuRepository.deleteById(menuId) == 0) {
            throw new BizException(ErrorCode.MENU_NOT_FOUND);
        }
    }

    /**
     * 查询指定父菜单下的所有直接子菜单。
     */
    private List<MenuTreeResponse> buildChildren(
            List<Menu> menus,
            Long parentId
    ) {
        return menus.stream()
                .filter(menu -> menu.parentId().equals(parentId))
                .sorted(
                        Comparator.comparingInt(Menu::orderNum)
                                .thenComparingLong(Menu::menuId)
                )
                .map(menu -> new MenuTreeResponse(
                        menu.menuId(),
                        menu.menuType(),
                        menu.menuName(),
                        menu.parentId(),
                        menu.path(),
                        menu.component(),
                        menu.orderNum(),
                        menu.perms(),
                        menu.icon(),
                        menu.status(),
                        menu.createdAt(),
                        menu.updatedAt(),
                        buildChildren(menus, menu.menuId())
                ))
                .toList();
    }

    /**
     * 父菜单 ID 为 0 时表示顶级菜单，否则父菜单必须存在。
     */
    private void validateParent(Long parentId) {
        if (parentId == 0L) {
            return;
        }

        if (menuRepository.findById(parentId).isEmpty()) {
            throw new BizException(ErrorCode.MENU_PARENT_NOT_FOUND);
        }
    }

    /**
     * 校验修改后的父菜单存在，且不是当前菜单或其后代菜单。
     */
    private void validateParentForUpdate(Long menuId, Long parentId) {
        if (parentId == 0L) {
            return;
        }
        if (menuId.equals(parentId)) {
            throw new BizException(ErrorCode.MENU_PARENT_INVALID);
        }

        List<Menu> menus = menuRepository.findAll();
        Long currentParentId = parentId;
        while (currentParentId != 0L) {
            Long targetId = currentParentId;
            Menu parent = menus.stream()
                    .filter(menu -> menu.menuId().equals(targetId))
                    .findFirst()
                    .orElseThrow(() -> new BizException(ErrorCode.MENU_PARENT_NOT_FOUND));
            if (parent.menuId().equals(menuId)) {
                throw new BizException(ErrorCode.MENU_PARENT_INVALID);
            }
            currentParentId = parent.parentId();
        }
    }

    /**
     * 根据菜单类型校验路由和权限字段。
     */
    private void validateFields(
            String menuType,
            String path,
            String component,
            String perms
    ) {
        if ("MENU".equals(menuType)
                && (!hasText(path) || !hasText(component))) {
            throw new BizException(ErrorCode.MENU_ROUTE_REQUIRED);
        }

        if ("BUTTON".equals(menuType) && !hasText(perms)) {
            throw new BizException(ErrorCode.MENU_PERMISSION_REQUIRED);
        }
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}


