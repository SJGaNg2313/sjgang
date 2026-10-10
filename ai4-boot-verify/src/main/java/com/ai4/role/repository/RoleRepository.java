package com.ai4.role.repository;

import com.ai4.role.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    /**
     * 查询全部角色。
     */
    List<Role> findAll();

    /**
     * 根据角色 ID 查询角色。
     */
    Optional<Role> findById(long roleId);

    /**
     * 判断角色编码是否已存在。
     */
    boolean existsByRoleCode(String roleCode);

    /**
     * 创建角色并返回生成的角色 ID。
     */
    long save(Role role);

    /**
     * 修改角色基本信息。
     */
    int update(Role role);

    /**
     * 修改角色状态。
     */
    int updateStatus(long roleId, int status);

    /**
     * 查询角色关联的用户数量。
     */
    int countUsersByRoleId(long roleId);

    /**
     * 删除角色的全部授权关系
     */
    int deleteMenuRelationsByRoleId(long roleId);

    /**
     * 删除角色。
     */
    int deleteById(long roleId);
}
