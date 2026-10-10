package com.ai4.classroom.repository;

import com.ai4.classroom.dto.ClassOptionResponse;
import com.ai4.classroom.model.ClassInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.util.List;
import java.util.Optional;

/** 使用 JdbcTemplate 查询班级。 */
@Repository
public class JdbcClassRepository implements ClassRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcClassRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ClassOptionResponse> findOptions() {
        return jdbcTemplate.query(
                """
                SELECT id, class_name
                FROM ai4_class
                WHERE del_flag = 1
                  AND class_status = 1
                ORDER BY class_name ASC, id ASC
                """,
                (resultSet, rowNumber) -> new ClassOptionResponse(
                        resultSet.getString("id"),
                        resultSet.getString("class_name")
                )
        );
    }

    @Override
    public Optional<ClassInfo> findActiveById(String classId) {
        List<ClassInfo> classes = jdbcTemplate.query(
                """
                SELECT
                    start_time,
                    package_id,
                    teacher_id,
                    room_type,
                    record,
                    close_delay
                FROM ai4_class
                WHERE id = ?
                  AND del_flag = 1
                  AND class_status = 1
                """,
                (resultSet, rowNumber) -> {
                    Time startTime = resultSet.getTime("start_time");
                    return new ClassInfo(
                            startTime == null ? null : startTime.toLocalTime(),
                            resultSet.getLong("package_id"),
                            resultSet.getString("teacher_id"),
                            resultSet.getObject("room_type", Integer.class),
                            resultSet.getObject("record", Integer.class),
                            resultSet.getObject("close_delay", Integer.class)
                    );
                },
                classId
        );
        return classes.stream().findFirst();
    }
}
