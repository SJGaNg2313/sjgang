package com.ai4.auth.repository;

import com.ai4.auth.model.RouteMenu;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 使用JdbcTemplate实现从数据库中读取角色和权限
 */
@Repository
public class JdbcAuthPermissionRepository implements AuthPermissionRepository{

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthPermissionRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据用户id查询启用状态下的角色名称
     */
    @Override
    public List<String> findRoleNamesByUserId(long userId){
        return jdbcTemplate.queryForList(
                """
                        select r.role_name
                        from sys_user_role ur
                        join sys_role r on ur.role_id = r.role_id
                        where ur.user_id = ?
                          and r.status = 0
                        order by r.role_sort, r.role_id
                    """,
                String.class,
                userId
        );
    }

    /**
     * 根据用户 ID 查询启用状态下的权限码。
     */
    @Override
    public List<String> findPermissionCodesByUserId(long userId) {
        return jdbcTemplate.queryForList(
                """
                select distinct m.perms
                from sys_user_role ur
                join sys_role r on ur.role_id = r.role_id
                join sys_role_menu rm on r.role_id = rm.role_id
                join sys_menu m on rm.menu_id = m.menu_id
                where ur.user_id = ?
                  and r.status = 0
                  and m.status = 0
                  and m.perms is not null
                  and m.perms <> ''
                order by m.perms
                """,
                String.class,
                userId
        );
    }

    /**
     * 根据用户 ID 查询当前用户可以访问的目录和菜单。
     */
    @Override
    public List<RouteMenu> findRoutesByUserId(long userId) {
        return jdbcTemplate.query(
                """
                select distinct
                    m.menu_id,
                    m.menu_type,
                    m.menu_name,
                    m.parent_id,
                    m.path,
                    m.component,
                    m.icon,
                    m.order_num
                from sys_user_role ur
                join sys_role r on ur.role_id = r.role_id
                join sys_role_menu rm on r.role_id = rm.role_id
                join sys_menu m on rm.menu_id = m.menu_id
                where ur.user_id = ?
                  and r.status = 0
                  and m.status = 0
                  and m.menu_type in ('DIR', 'MENU')
                order by m.order_num, m.menu_id
                """,
                (resultSet, rowNum) -> new RouteMenu(
                        resultSet.getLong("menu_id"),
                        resultSet.getString("menu_type"),
                        resultSet.getString("menu_name"),
                        resultSet.getLong("parent_id"),
                        resultSet.getString("path"),
                        resultSet.getString("component"),
                        resultSet.getString("icon"),
                        resultSet.getInt("order_num")
                ),
                userId
        );
    }
}
