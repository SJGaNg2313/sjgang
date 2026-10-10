package com.ai4.menu.repository;

import com.ai4.menu.model.Menu;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * 使用 JdbcTemplate 访问 MySQL sys_menu 表。
 */
@Repository
public class JdbcMenuRepository implements MenuRepository{

    private final JdbcTemplate jdbcTemplate;

    private static final String MENU_COLUMNS = """
          menu_id, menu_type, menu_name, parent_id,
          path, component, order_num, perms, icon,
          status, created_at, updated_at
          """;

    public JdbcMenuRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 公共映射方法
     */
    private RowMapper<Menu> menuRowMapper() {
        return (resultSet, rowNumber) -> new Menu(
                resultSet.getLong("menu_id"),
                resultSet.getString("menu_type"),
                resultSet.getString("menu_name"),
                resultSet.getLong("parent_id"),
                resultSet.getString("path"),
                resultSet.getString("component"),
                resultSet.getInt("order_num"),
                resultSet.getString("perms"),
                resultSet.getString("icon"),
                resultSet.getInt("status"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    /**
     * 根据菜单ID查询菜单详情
     */
    @Override
    public Optional<Menu> findById(Long menuId){
        List<Menu> menus = jdbcTemplate.query(
                "select " + MENU_COLUMNS + " from sys_menu where menu_id = ?",
                menuRowMapper(),
                menuId
        );
        return menus.stream().findFirst();
    }

    /**
     * 查询全部菜单
     */
    @Override
    public List<Menu> findAll(){
        return jdbcTemplate.query(
                "select menu_id, menu_type, menu_name, parent_id, path, component, order_num, perms, icon, status, created_at, updated_at from sys_menu order by order_num , menu_id ",
                menuRowMapper()
        );
    }

    /**
     * 新增菜单并返回保存后完整的数据
     */
    @Override
    public Menu save(Menu menu){
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into sys_menu(
                                menu_type,
                                menu_name,
                                parent_id,
                                path,
                                component,
                                order_num,
                                perms,
                                icon,
                                status
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                            Statement.RETURN_GENERATED_KEYS
                    );

                    statement.setString(1, menu.menuType());
                    statement.setString(2, menu.menuName());
                    statement.setLong(3, menu.parentId());
                    statement.setString(4, menu.path());
                    statement.setString(5, menu.component());
                    statement.setInt(6, menu.orderNum());
                    statement.setString(7, menu.perms());
                    statement.setString(8, menu.icon());
                    statement.setInt(9, menu.status());

                    return statement;
                },
                keyHolder
        );

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException("Failed to obtain saved menu ID");
        }

        return findById(generatedId.longValue())
                .orElseThrow(() ->
                        new IllegalStateException("Failed to query saved menu")
                );
    }

    /**
     * 修改指定菜单的可维护字段
     */
    @Override
    public int update(Menu menu) {
        return jdbcTemplate.update(
                """
                update sys_menu
                set menu_type = ?, menu_name = ?, parent_id = ?, path = ?,
                    component = ?, order_num = ?, perms = ?, icon = ?, status = ?
                where menu_id = ?
                """,
                menu.menuType(), menu.menuName(), menu.parentId(), menu.path(),
                menu.component(), menu.orderNum(), menu.perms(), menu.icon(),
                menu.status(), menu.menuId()
        );
    }

    /**
     * 查询指定菜单是否仍有直接子菜单
     */
    @Override
    public long countByParentId(Long parentId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from sys_menu where parent_id = ?",
                Long.class,
                parentId
        );
        return count == null ? 0 : count;
    }

    @Override
    public long countRoleRelations(Long menuId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from sys_role_menu where menu_id = ?",
                Long.class,
                menuId
        );
        return count == null ? 0 : count;
    }

    /** 新菜单默认授权给启用的超级管理员角色。 */
    @Override
    public int assignToSuperAdmin(Long menuId) {
        return jdbcTemplate.update(
                """
                insert ignore into sys_role_menu(role_id, menu_id)
                select role_id, ?
                from sys_role
                where role_code = 'super_admin' and status = 0
                """,
                menuId
        );
    }

    /**
     * 根据菜单 ID 物理删除菜单
     */
    @Override
    public int deleteById(Long menuId) {
        return jdbcTemplate.update("delete from sys_menu where menu_id = ?", menuId);
    }
}
