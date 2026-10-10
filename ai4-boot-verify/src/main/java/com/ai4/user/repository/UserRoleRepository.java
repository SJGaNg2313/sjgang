package com.ai4.user.repository;

import java.util.Collection;
import java.util.List;

/**
 * 用户角色授权数据访问接口。
 */
public interface UserRoleRepository {

    /**
     * 查询用户当前关联的角色 ID。
     */
    List<Long> findRoleIdsByUserId(long userId);

    /**
     * 删除用户现有的全部角色关系。
     */
    int deleteByUserId(long userId);

    /**
     * 批量写入用户角色关系。
     */
    void batchInsert(long userId, Collection<Long> roleIds);

    /**
     * 查询拥有指定角色编码的启用用户数量。
     */
    int countActiveUsersByRoleCode(String roleCode);
}
