package com.ai4.role.repository;

import com.ai4.role.model.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcRoleRepository implements RoleRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Role> roleRowMapper = (resultSet, rowNum) ->
            new Role(
                    resultSet.getLong("role_id"),
                    resultSet.getString("role_name"),
                    resultSet.getString("role_code"),
                    resultSet.getInt("status"),
                    resultSet.getInt("role_sort"),
                    resultSet.getTimestamp("created_at").toLocalDateTime(),
                    resultSet.getTimestamp("updated_at").toLocalDateTime()
            );

    public JdbcRoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按显示顺序和角色ID查询全部角色
     */
    @Override
    public List<Role> findAll(){
        return jdbcTemplate.query(
                "select role_id,role_name,role_code,status,role_sort,created_at,updated_at from sys_role order by role_sort,role_id",
                roleRowMapper
        );
    }

    /**
     * 根据角色ID查询角色
     */
    @Override
    public Optional<Role> findById(long roleId){
        List<Role> roles = jdbcTemplate.query(
                "select role_id,role_name,role_code,status,role_sort,created_at,updated_at from sys_role where role_id = ?",
                roleRowMapper,
                roleId
        );
        return roles.stream().findFirst();
    }

    /**
     * 判断角色编码是否存在
     */
    @Override
    public boolean existsByRoleCode(String roleCode){
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_role where role_code = ?",
                Integer.class,
                roleCode
        );
        return count != null && count > 0;
    }

    /**
     * 创建角色并返回生成的角色 ID。
     */
    @Override
    public long save(Role role) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "insert into sys_role (role_name,role_code,status,role_sort) values (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, role.roleName());
            statement.setString(2, role.roleCode());
            statement.setInt(3, role.status());
            statement.setInt(4, role.roleSort());

            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建角色后未获取到角色 ID");
        }

        return key.longValue();
    }

    /**
     * 修改角色名称和显示顺序。
     */
    @Override
    public int update(Role role) {
        return jdbcTemplate.update(
                "update sys_role set role_name = ?, role_sort = ? where role_id = ?",
                role.roleName(),
                role.roleSort(),
                role.roleId()
        );
    }

    /**
     * 修改角色状态。
     */
    @Override
    public int updateStatus(long roleId, int status) {
        return jdbcTemplate.update(
                "update sys_role set status = ? where role_id = ?",
                status,
                roleId
        );
    }

    /**
     * 查询角色关联的用户数量。
     */
    @Override
    public int countUsersByRoleId(long roleId) {
        Integer count = jdbcTemplate.queryForObject(
                " select count(*) from sys_user_role where role_id = ?",
                Integer.class,
                roleId
        );

        return count == null ? 0 : count;
    }

    /**
     * 删除角色的所有授权关系
     */
    @Override
    public int deleteMenuRelationsByRoleId(long roleId){
        return jdbcTemplate.update(
                "delete from sys_role_menu where role_id = ?",
                roleId
        );
    }

    /**
     * 删除角色。
     */
    @Override
    public int deleteById(long roleId) {
        return jdbcTemplate.update(
                " delete from sys_role where role_id = ?",
                roleId
        );
    }
}


