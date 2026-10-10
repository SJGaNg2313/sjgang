package com.ai4.menu.repository;

import com.ai4.menu.model.Menu;

import java.util.List;
import java.util.Optional;

/**
 * 菜单数据访问接口
 */
public interface MenuRepository {

    /**
     * 根据菜单id查询菜单
     */
    Optional<Menu> findById(Long menuId);

    /**
     * 查询全部菜单
     */
    List<Menu> findAll();

    /**
     * 保存菜单并返回完整数据
     */
    Menu save(Menu menu);

    /**
     * 修改菜单并返回 SQL 影响的记录数
     */
    int update(Menu menu);

    /**
     * 查询指定父菜单下的直接子菜单数量
     */
    long countByParentId(Long parentId);

    /** 查询指定菜单已关联的角色数量。 */
    long countRoleRelations(Long menuId);

    /** 将菜单授权给超级管理员角色；角色不存在时不写入。 */
    int assignToSuperAdmin(Long menuId);

    /**
     * 根据菜单 ID 删除菜单并返回 SQL 影响的记录数
     */
    int deleteById(Long menuId);

}
