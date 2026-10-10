package com.ai4.teacher.repository;

import com.ai4.teacher.dto.TeacherListItemResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/** 使用 JdbcTemplate 查询教师列表。 */
@Repository
public class JdbcTeacherRepository implements TeacherRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcTeacherRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TeacherListItemResponse> findAll(String userName, String nickName) {
        StringBuilder sql = new StringBuilder("""
                select t.id, t.user_id, u.user_name, t.nickname
                from live_teach t
                join sys_user u on u.user_id = t.user_id
                where t.del_flag = 1
                """);
        List<Object> arguments = new ArrayList<>();

        if (userName != null) {
            sql.append(" and u.user_name like ?");
            arguments.add("%" + userName + "%");
        }
        if (nickName != null) {
            sql.append(" and t.nickname like ?");
            arguments.add("%" + nickName + "%");
        }
        sql.append(" order by t.id");

        return jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> new TeacherListItemResponse(
                        resultSet.getString("id"),
                        resultSet.getLong("user_id"),
                        resultSet.getString("user_name"),
                        resultSet.getString("nickname")
                ),
                arguments.toArray()
        );
    }
}
