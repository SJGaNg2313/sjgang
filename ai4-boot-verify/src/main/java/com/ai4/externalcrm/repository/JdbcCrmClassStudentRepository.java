package com.ai4.externalcrm.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 使用 JdbcTemplate 实现 CRM 班级学员同步。
 */
@Repository
public class JdbcCrmClassStudentRepository implements CrmClassStudentRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcCrmClassStudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> findUserIdsByPhones(List<String> phones) {
        if (phones.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(phones.size(), "?"));
        List<Map.Entry<String, String>> rows = jdbcTemplate.query(
                "SELECT phone, user_id FROM el_user WHERE del_flag = 1 "
                        + "AND phone IN (" + placeholders + ")",
                (resultSet, rowNumber) -> Map.entry(
                        resultSet.getString("phone"), resultSet.getString("user_id")
                ),
                phones.toArray()
        );
        Map<String, String> users = new HashMap<>();
        rows.forEach(row -> users.putIfAbsent(row.getKey(), row.getValue()));
        return users;
    }

    @Override
    public Set<String> findActiveUserIdsByClassId(String classId) {
        return new HashSet<>(jdbcTemplate.query(
                "SELECT user_id FROM ai4_class_student WHERE class_id = ? AND state = 1",
                (resultSet, rowNumber) -> resultSet.getString("user_id"),
                classId
        ));
    }

    @Override
    public void bind(String bindingId, String classId, String userId) {
        jdbcTemplate.update(
                """
                INSERT INTO ai4_class_student(
                    id, class_id, user_id, join_time, state, created_at, updated_at
                ) VALUES (?, ?, ?, NOW(), 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    state = 1,
                    join_time = NOW(),
                    updated_at = NOW()
                """,
                bindingId,
                classId,
                userId
        );
    }

    @Override
    public int remove(String classId, String userId) {
        return jdbcTemplate.update(
                "UPDATE ai4_class_student SET state = 2, updated_at = NOW() "
                        + "WHERE class_id = ? AND user_id = ? AND state = 1",
                classId,
                userId
        );
    }
}
