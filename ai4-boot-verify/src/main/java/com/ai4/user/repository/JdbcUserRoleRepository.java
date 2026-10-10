package com.ai4.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class JdbcUserRoleRepository implements UserRoleRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询用户当前关联的角色 ID。
     */
    @Override
    public List<Long> findRoleIdsByUserId(long userId) {
        return jdbcTemplate.queryForList(
                "select role_id from sys_user_role where user_id = ? order by role_id",
                Long.class,
                userId
        );
    }

    /**
     * 删除用户现有的全部角色关系。
     */
    @Override
    public int deleteByUserId(long userId) {
        return jdbcTemplate.update(
                "delete from sys_user_role where user_id = ?",
                userId
        );
    }

    /**
     * 批量写入用户角色关系。
     */
    @Override
    public void batchInsert(long userId, Collection<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }

        List<Object[]> batchArguments = roleIds.stream()
                .map(roleId -> new Object[]{userId, roleId})
                .toList();

        jdbcTemplate.batchUpdate(
                "insert into sys_user_role ( user_id, role_id) values (?, ?)",
                batchArguments
        );
    }

    /**
     * 查询拥有指定角色编码的启用用户数量。
     */
    @Override
    public int countActiveUsersByRoleCode(String roleCode) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(distinct u.user_id)
                from sys_user u
                join sys_user_role ur on u.user_id = ur.user_id
                join sys_role r on ur.role_id = r.role_id
                where r.role_code = ?
                  and u.status = 0
                  and r.status = 0
                """,
                Integer.class,
                roleCode
        );

        return count == null ? 0 : count;
    }
}
