package com.ai4.externalcrm.repository;

import com.ai4.externalcrm.dto.CrmCoursePackageStatResponse;
import com.ai4.externalcrm.model.CrmClassCreateData;
import com.ai4.externalcrm.model.CrmClassSchedule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/** 使用 JdbcTemplate 同步 CRM 班级。 */
@Repository
public class JdbcCrmClassRepository implements CrmClassRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCrmClassRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> findClassIdByCrmClassId(Integer crmClassId) {
        return jdbcTemplate.query(
                "SELECT id FROM ai4_class WHERE class_id = ? AND del_flag = 1 LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getString("id"),
                crmClassId
        ).stream().findFirst();
    }

    @Override
    public Optional<String> findTeacherIdByCrmTeacherId(Integer crmTeacherId) {
        return jdbcTemplate.query(
                "SELECT id FROM live_teach WHERE crm_teacher_id = ? AND del_flag = 1 LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getString("id"),
                crmTeacherId
        ).stream().findFirst();
    }

    @Override
    public Optional<CrmClassSchedule> findScheduleByCrmClassId(Integer crmClassId) {
        List<CrmClassSchedule> schedules = jdbcTemplate.query(
                """
                SELECT
                    id,
                    package_id,
                    start_time,
                    end_time,
                    start_class_time,
                    course_rule,
                    teacher_id,
                    room_type,
                    record,
                    close_delay,
                    class_status
                FROM ai4_class
                WHERE class_id = ?
                  AND del_flag = 1
                LIMIT 1
                """,
                (resultSet, rowNumber) -> {
                    Time startTime = resultSet.getTime("start_time");
                    Time endTime = resultSet.getTime("end_time");
                    Timestamp startClassTime = resultSet.getTimestamp("start_class_time");
                    return new CrmClassSchedule(
                            resultSet.getString("id"),
                            resultSet.getLong("package_id"),
                            startTime == null ? null : startTime.toLocalTime(),
                            endTime == null ? null : endTime.toLocalTime(),
                            startClassTime == null ? null : startClassTime.toLocalDateTime(),
                            resultSet.getObject("course_rule", Integer.class),
                            resultSet.getString("teacher_id"),
                            resultSet.getObject("room_type", Integer.class),
                            resultSet.getObject("record", Integer.class),
                            resultSet.getObject("close_delay", Integer.class),
                            resultSet.getObject("class_status", Integer.class)
                    );
                },
                crmClassId
        );
        return schedules.stream().findFirst();
    }

    @Override
    public void save(CrmClassCreateData data) {
        jdbcTemplate.update(
                """
                INSERT INTO ai4_class(
                    id,
                    class_id,
                    class_name,
                    start_time,
                    end_time,
                    package_id,
                    teacher_id,
                    class_type,
                    room_type,
                    record,
                    course_rule,
                    start_class_time,
                    class_status,
                    del_flag,
                    close_delay,
                    unlock_interval
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                """,
                data.classId(),
                data.crmClassId(),
                data.className(),
                data.startTime(),
                data.endTime(),
                data.packageId(),
                data.teacherId(),
                data.classType(),
                data.roomType(),
                data.record(),
                data.courseRule(),
                data.startClassTime(),
                data.classStatus(),
                data.closeDelay(),
                data.unlockInterval()
        );
    }

    @Override
    public int update(CrmClassCreateData data) {
        return jdbcTemplate.update(
                """
                UPDATE ai4_class
                SET class_name = ?, start_time = ?, end_time = ?, package_id = ?,
                    teacher_id = ?, class_type = ?, room_type = ?, record = ?,
                    course_rule = ?, start_class_time = ?, class_status = ?, close_delay = ?
                WHERE id = ? AND del_flag = 1
                """,
                data.className(), data.startTime(), data.endTime(), data.packageId(),
                data.teacherId(), data.classType(), data.roomType(), data.record(),
                data.courseRule(), data.startClassTime(), data.classStatus(),
                data.closeDelay(), data.classId()
        );
    }

    @Override
    public int updateStatus(String classId, Integer classStatus) {
        return jdbcTemplate.update(
                "UPDATE ai4_class SET class_status = ? WHERE id = ? AND del_flag = 1",
                classStatus,
                classId
        );
    }

    @Override
    public long findLiveCourseCountByPackageId(long packageId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT live_course.course_id)
                FROM ai4_class class_info
                JOIN ai4_class_live_course relation
                  ON relation.class_id = class_info.id
                JOIN live_course live_course
                  ON live_course.course_id = relation.course_id
                 AND live_course.del_flag = 1
                WHERE class_info.package_id = ?
                  AND class_info.del_flag = 1
                """,
                Long.class,
                packageId
        );
        return count == null ? 0 : count;
    }

    @Override
    public List<CrmCoursePackageStatResponse> findCoursePackageStats() {
        return jdbcTemplate.query(
                """
                SELECT course_package.id AS package_id, course_package.package_name,
                       COUNT(DISTINCT live_course.course_id) AS live_course_count
                FROM ai4_course_package course_package
                LEFT JOIN ai4_class class_info
                  ON class_info.package_id = course_package.id
                 AND class_info.del_flag = 1
                LEFT JOIN ai4_class_live_course relation
                  ON relation.class_id = class_info.id
                LEFT JOIN live_course live_course
                  ON live_course.course_id = relation.course_id
                 AND live_course.del_flag = 1
                WHERE course_package.del_flag = 1
                GROUP BY course_package.id, course_package.package_name, course_package.sort_order
                ORDER BY course_package.sort_order ASC, course_package.id ASC
                """,
                (resultSet, rowNumber) -> new CrmCoursePackageStatResponse(
                        resultSet.getLong("package_id"),
                        resultSet.getString("package_name"),
                        resultSet.getLong("live_course_count")
                )
        );
    }
}
