package com.ai4.course.repository;

import com.ai4.course.model.CoursePackage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 使用 JdbcTemplate 查询系统中唯一的有效课包。 */
@Repository
public class JdbcCoursePackageRepository implements CoursePackageRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCoursePackageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CoursePackage> findOnlyActive() {
        List<CoursePackage> packages = jdbcTemplate.query(
                """
                SELECT id, package_name, sort_order, del_flag, remark, created_at, updated_at
                FROM ai4_course_package
                WHERE del_flag = 1
                ORDER BY id
                LIMIT 2
                """,
                (resultSet, rowNumber) -> new CoursePackage(
                        resultSet.getLong("id"),
                        resultSet.getString("package_name"),
                        resultSet.getInt("sort_order"),
                        resultSet.getInt("del_flag"),
                        resultSet.getString("remark"),
                        resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getTimestamp("updated_at").toLocalDateTime()
                )
        );
        return packages.size() == 1 ? Optional.of(packages.get(0)) : Optional.empty();
    }
}
