package com.ai4.account.repository;

import com.ai4.account.dto.AccountListQuery;
import com.ai4.account.dto.AccountListItemResponse;
import com.ai4.account.model.AccountCourseDetailData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 使用 JdbcTemplate 访问本地 el_user 表。
 */
@Repository
public class JdbcAccountRepository implements AccountRepository {

    private static final String ACCOUNT_LIST_FROM = """
            FROM el_user account
            LEFT JOIN (
                SELECT user_id, class_name, package_name
                FROM (
                    SELECT
                        class_student.user_id,
                        class_info.class_name,
                        course_package.package_name,
                        ROW_NUMBER() OVER (
                            PARTITION BY class_student.user_id
                            ORDER BY class_student.updated_at DESC, class_student.id DESC
                        ) AS row_num
                    FROM ai4_class_student class_student
                    JOIN ai4_class class_info
                      ON class_info.id = class_student.class_id
                     AND class_info.del_flag = 1
                    JOIN ai4_course_package course_package
                      ON course_package.id = class_info.package_id
                     AND course_package.del_flag = 1
                    WHERE class_student.state = 1
                ) ranked_membership
                WHERE row_num = 1
            ) membership
              ON membership.user_id = account.user_id
            WHERE account.del_flag = 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AccountCourseDetailData> findDetailByUserId(String userId) {
        return jdbcTemplate.query(
                """
                SELECT
                    account.user_id,
                    account.phone,
                    membership.class_name,
                    latest_course.course_id,
                    latest_course.project_name,
                    latest_course.teacher_name,
                    latest_course.course_time,
                    latest_course.status,
                    latest_course.start_time,
                    latest_course.end_time,
                    CASE
                        WHEN latest_course.course_id IS NULL THEN NULL
                        WHEN latest_course.status = 0 THEN NULL
                        WHEN attendance.be_duty = 1 OR attendance.join_time IS NOT NULL THEN 1
                        ELSE 0
                    END AS attended
                FROM el_user account
                LEFT JOIN (
                    SELECT user_id, class_id, class_name
                    FROM (
                        SELECT
                            class_student.user_id,
                            class_student.class_id,
                            class_info.class_name,
                            ROW_NUMBER() OVER (
                                PARTITION BY class_student.user_id
                                ORDER BY class_student.updated_at DESC, class_student.id DESC
                            ) AS row_num
                        FROM ai4_class_student class_student
                        JOIN ai4_class class_info
                          ON class_info.id = class_student.class_id
                         AND class_info.del_flag = 1
                        WHERE class_student.state = 1
                    ) ranked_membership
                    WHERE row_num = 1
                ) membership
                  ON membership.user_id = account.user_id
                LEFT JOIN (
                    SELECT
                        class_id,
                        course_id,
                        project_name,
                        teacher_name,
                        course_time,
                        status,
                        start_time,
                        end_time
                    FROM (
                        SELECT
                            relation.class_id,
                            course.course_id,
                            project.project_name,
                            COALESCE(NULLIF(teacher.nickname, ''), teacher.username, '') AS teacher_name,
                            course.course_time,
                            course.status,
                            course.start_time,
                            course.end_time,
                            ROW_NUMBER() OVER (
                                PARTITION BY relation.class_id
                                ORDER BY course.course_time DESC, course.course_id DESC
                            ) AS row_num
                        FROM ai4_class_live_course relation
                        JOIN live_course course
                          ON course.course_id = relation.course_id
                         AND course.del_flag = 1
                         AND course.status IN (0, 1, 2, 3)
                        JOIN ai4_project project
                          ON project.id = relation.project_id
                         AND project.del_flag = 1
                        LEFT JOIN live_teach teacher
                          ON teacher.id = course.teacher_id
                         AND teacher.del_flag = 1
                    ) ranked_course
                    WHERE row_num = 1
                ) latest_course
                  ON latest_course.class_id = membership.class_id
                LEFT JOIN live_user_course_record attendance
                  ON attendance.course_id = latest_course.course_id
                 AND attendance.user_id = account.user_id
                WHERE account.user_id = ?
                  AND account.del_flag = 1
                LIMIT 1
                """,
                (resultSet, rowNumber) -> {
                    Integer attended = resultSet.getObject("attended", Integer.class);
                    return new AccountCourseDetailData(
                            resultSet.getString("user_id"),
                            resultSet.getString("phone"),
                            resultSet.getString("class_name"),
                            resultSet.getString("course_id"),
                            resultSet.getString("project_name"),
                            resultSet.getString("teacher_name"),
                            resultSet.getObject("course_time", Long.class),
                            resultSet.getObject("status", Integer.class),
                            resultSet.getObject("start_time", Long.class),
                            resultSet.getObject("end_time", Long.class),
                            attended == null ? null : attended == 1
                    );
                },
                userId
        ).stream().findFirst();
    }

    @Override
    public long count(AccountListQuery query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) ").append(ACCOUNT_LIST_FROM);
        List<Object> parameters = new ArrayList<>();
        appendFilters(sql, parameters, query);

        Long count = jdbcTemplate.queryForObject(
                sql.toString(),
                Long.class,
                parameters.toArray()
        );
        return count == null ? 0 : count;
    }

    @Override
    public List<AccountListItemResponse> findPage(AccountListQuery query, int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                """
                SELECT
                    account.user_id,
                    account.nick,
                    account.phone,
                    membership.class_name,
                    membership.package_name,
                    CASE
                        WHEN membership.user_id IS NULL THEN '普通'
                        ELSE '正式'
                    END AS identity,
                    account.devicemodel,
                    account.devicesysver,
                    account.signuptime,
                    account.login_time
                """
        );
        sql.append(ACCOUNT_LIST_FROM);
        List<Object> parameters = new ArrayList<>();
        appendFilters(sql, parameters, query);
        sql.append(" ORDER BY account.signuptime DESC, account.id DESC LIMIT ? OFFSET ?");
        parameters.add(limit);
        parameters.add(offset);

        return jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> {
                    Timestamp signupTime = resultSet.getTimestamp("signuptime");
                    Timestamp loginTime = resultSet.getTimestamp("login_time");
                    return new AccountListItemResponse(
                            resultSet.getString("user_id"),
                            resultSet.getString("nick"),
                            resultSet.getString("phone"),
                            resultSet.getString("class_name"),
                            resultSet.getString("package_name"),
                            resultSet.getString("identity"),
                            resultSet.getString("devicemodel"),
                            resultSet.getString("devicesysver"),
                            signupTime == null ? null : signupTime.toLocalDateTime(),
                            loginTime == null ? null : loginTime.toLocalDateTime()
                    );
                },
                parameters.toArray()
        );
    }

    private void appendFilters(
            StringBuilder sql,
            List<Object> parameters,
            AccountListQuery query
    ) {
        if (hasText(query.userId())) {
            sql.append(" AND account.user_id LIKE ?");
            parameters.add("%" + query.userId().trim() + "%");
        }
        if (hasText(query.nick())) {
            sql.append(" AND account.nick LIKE ?");
            parameters.add("%" + query.nick().trim() + "%");
        }
        if (hasText(query.identity())) {
            sql.append(" AND CASE WHEN membership.user_id IS NULL THEN '普通' ELSE '正式' END LIKE ?");
            parameters.add("%" + query.identity().trim() + "%");
        }
        if (hasText(query.phone())) {
            sql.append(" AND account.phone LIKE ?");
            parameters.add("%" + query.phone().trim() + "%");
        }
        if (hasText(query.className())) {
            sql.append(" AND membership.class_name LIKE ?");
            parameters.add("%" + query.className().trim() + "%");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
