package com.ai4.user.repository;

import com.ai4.user.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 使用 JdbcTemplate 访问 MySQL users 表。
 */
@Repository
public class JdbcUserRepository implements UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据用户Id查询用户
     */
    @Override
    public Optional<User> findById(Long userId) {
        List<User> users = jdbcTemplate.query(
                "select user_id, user_name, password,status from sys_user where user_id = ? ",
                (resultSet, rowNum) -> new User(
                        resultSet.getLong("user_id"),
                        resultSet.getString("user_name"),
                        resultSet.getString("password"),
                        resultSet.getInt("status")
                ),
                userId
        );

        return users.stream().findFirst();
    }

    /**
     * 判断指定用户名是否已存在。
     */
    @Override
    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sys_user where user_name = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

    /**
     * 根据用户名查询用户，未查询到时返回 Optional.empty()。
     */
    @Override
    public Optional<User> findByUsername(String userName) {
        List<User> users = jdbcTemplate.query(
                "select user_id, user_name, password, status from sys_user where user_name = ?",
                (resultSet, rowNumber) -> new User(
                        resultSet.getLong("user_id"),
                        resultSet.getString("user_name"),
                        resultSet.getString("password"),
                        resultSet.getInt("status")
                ),
                userName
        );

        return users.stream().findFirst();
    }

    /**
     * 保存用户后重新查询，返回包含数据库 ID 的完整对象。
     */
    @Override
    public User save(String username, String password) {
        jdbcTemplate.update(
                "insert into sys_user(user_name,password) values (?,?)",
                username,
                password
        );

        return findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Failed to query saved user"));
    }

    /**
     * 查询后台所有用户
     */
    @Override
    public List<User> findAll(){
        return jdbcTemplate.query(
                "select user_id, user_name, status from sys_user order by user_id ",
                (resultSet,rowNumber) -> new User(
                        resultSet.getLong("user_id"),
                        resultSet.getString("user_name"),
                        null,
                        resultSet.getInt("status")
                )
        );
    }

    /**
     * 修改对应id下的用户状态
     */
    @Override
    public int updateStatus(Long userId,Integer status){
        return jdbcTemplate.update(
                "update sys_user set status = ? where user_id = ?",
                status,
                userId
        );
    }

    /**
     * 重置后台用户密码
     */
    @Override
    public int updatePassword(Long userId, String encodedPassword){
        return jdbcTemplate.update(
                "update sys_user set password = ? where user_id = ?",
                encodedPassword,
                userId
        );
    }

    @Override
    public int updateCredentials(Long userId, String userName, String encodedPassword) {
        return jdbcTemplate.update(
                """
                update sys_user
                set user_name = ?, password = coalesce(?, password)
                where user_id = ?
                """,
                userName,
                encodedPassword,
                userId
        );
    }

    @Override
    public int updateLinkedTeacherPassword(Long userId, String encodedPassword) {
        return jdbcTemplate.update(
                "update live_teach set password = ? where user_id = ? and del_flag = 1",
                encodedPassword,
                userId
        );
    }
}
