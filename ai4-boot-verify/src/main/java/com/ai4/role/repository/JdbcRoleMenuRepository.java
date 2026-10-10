package com.ai4.role.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class JdbcRoleMenuRepository implements RoleMenuRepository{

    private final JdbcTemplate jdbcTemplate;

    public JdbcRoleMenuRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询角色当前关联的全部菜单 ID。
     */
    @Override
    public List<Long> findMenuIdsByRoleId(long roleId) {
        return jdbcTemplate.queryForList(
                """
                select menu_id
                from sys_role_menu
                where role_id = ?
                order by menu_id
                """,
                Long.class,
                roleId
        );
    }

    /**
     * 删除角色现有的全部菜单授权。
     */
    @Override
    public int deleteByRoleId(long roleId) {
        return jdbcTemplate.update(
                """
                delete from sys_role_menu
                where role_id = ?
                """,
                roleId
        );
    }

    /**
     * 批量写入角色菜单授权。
     */
    @Override
    public void batchInsert(long roleId, Collection<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return;
        }

        List<Object[]> batchArguments = menuIds.stream()
                .map(menuId -> new Object[]{roleId, menuId})
                .toList();

        jdbcTemplate.batchUpdate(
                "insert into sys_role_menu ( role_id, menu_id) values (?, ?)",
                batchArguments
        );
    }
}
