package com.ai4.role.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.menu.model.Menu;
import com.ai4.menu.repository.MenuRepository;
import com.ai4.role.model.Role;
import com.ai4.role.repository.RoleMenuRepository;
import com.ai4.role.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoleMenuService {

    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;

    public RoleMenuService(
            RoleRepository roleRepository,
            RoleMenuRepository roleMenuRepository,
            MenuRepository menuRepository
    ){
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuRepository = menuRepository;
    }

    /**
     * 查询角色当前关联的菜单ID
     */
    public List<Long> findMenuIds(long roleId){
        getRole(roleId);
        return roleMenuRepository.findMenuIdsByRoleId(roleId);
    }

    /**
     * 查询角色菜单授权，并自动补齐所有父菜单
     */
    @Transactional
    public List<Long> replaceMenus(long roleId,List<Long> requestedMenuIds){
        Role role = getRole(roleId);

        if("super_admin".equals(role.roleCode()) && requestedMenuIds.isEmpty()){
            throw new BizException(ErrorCode.SUPER_ADMIN_PROTECTED);
        }

        List<Menu> allMenus = menuRepository.findAll();
        Map<Long, Menu> menuMap = allMenus.stream()
                .collect(Collectors.toMap(
                        Menu::menuId,
                        Function.identity()
                ));

        LinkedHashSet<Long> completedMenuIds = new LinkedHashSet<>();

        for (Long menuId : requestedMenuIds) {
            addMenuAndParents(menuId, menuMap, completedMenuIds);
        }

        roleMenuRepository.deleteByRoleId(roleId);
        roleMenuRepository.batchInsert(roleId, completedMenuIds);

        return completedMenuIds.stream()
                .sorted()
                .toList();
    }

    /**
     * 添加指定菜单，并沿父级关系补齐所有祖先菜单。
     */
    private void addMenuAndParents(
            Long menuId,
            Map<Long, Menu> menuMap,
            Set<Long> completedMenuIds
    ) {
        Long currentMenuId = menuId;
        Set<Long> visitedMenuIds = new HashSet<>();

        while (currentMenuId != 0L) {
            if (!visitedMenuIds.add(currentMenuId)) {
                throw new BizException(ErrorCode.MENU_PARENT_INVALID);
            }

            Menu menu = menuMap.get(currentMenuId);
            if (menu == null) {
                throw new BizException(ErrorCode.MENU_NOT_FOUND);
            }

            completedMenuIds.add(menu.menuId());
            currentMenuId = menu.parentId();
        }
    }

    /**
     * 查询角色，不存在时抛出业务异常。
     */
    private Role getRole(long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(ErrorCode.ROLE_NOT_FOUND));
    }
}
