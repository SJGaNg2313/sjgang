package com.ai4.role.repository;

import java.util.Collection;
import java.util.List;

/**
 * 角色菜单授权数据访问接口
 */
public interface RoleMenuRepository {

    /**
     * 查询当前用户关联的全部菜单ID
     */
    List<Long> findMenuIdsByRoleId(long roleId);

    /**
     * 删除角色现有的全部菜单授权
     */
    int deleteByRoleId(long roleId);

    /**
     * 批量写入角色菜单授权
     */
    void batchInsert(long roleId, Collection<Long> menuIds);
}
