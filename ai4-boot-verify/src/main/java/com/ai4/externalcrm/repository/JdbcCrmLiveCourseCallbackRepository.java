package com.ai4.externalcrm.repository;

import com.ai4.externalcrm.model.CrmCourseEndData;
import com.ai4.externalcrm.model.CrmStudentAttendanceData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 使用 JdbcTemplate 查询 CRM 直播课结束回调数据。 */
@Repository
public class JdbcCrmLiveCourseCallbackRepository implements CrmLiveCourseCallbackRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCrmLiveCourseCallbackRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CrmCourseEndData> findCourse(String courseId) {
        return jdbcTemplate.query(
                """
                SELECT
                    course.course_id,
                    class_info.class_id AS crm_class_id,
                    course.course_time,
                    course.course_duration,
                    course.status,
                    course.record,
                    video.m3u8 AS m3u8_urls
                FROM live_course course
                JOIN ai4_class_live_course relation
                  ON relation.course_id = course.course_id
                JOIN ai4_class class_info
                  ON class_info.id = relation.class_id
                 AND class_info.del_flag = 1
                LEFT JOIN live_course_video video
                  ON video.course_id = course.course_id
                WHERE course.course_id = ?
                  AND course.del_flag = 1
                LIMIT 1
                """,
                (resultSet, rowNumber) -> new CrmCourseEndData(
                        resultSet.getString("course_id"),
                        resultSet.getInt("crm_class_id"),
                        resultSet.getLong("course_time"),
                        resultSet.getInt("course_duration"),
                        resultSet.getInt("status"),
                        resultSet.getInt("record"),
                        resultSet.getString("m3u8_urls")
                ),
                courseId
        ).stream().findFirst();
    }

    @Override
    public List<CrmStudentAttendanceData> findStudentAttendances(String courseId) {
        return jdbcTemplate.query(
                """
                SELECT
                    student.phone,
                    attendance.join_time,
                    attendance.leave_time,
                    COALESCE(attendance.be_duty, 0) AS is_attendance,
                    COALESCE(attendance.be_late, 0) AS is_late,
                    COALESCE(attendance.leave_early, 0) AS is_leave_early
                FROM live_course course
                JOIN ai4_class_live_course relation
                  ON relation.course_id = course.course_id
                JOIN ai4_class class_info
                  ON class_info.id = relation.class_id
                 AND class_info.del_flag = 1
                JOIN ai4_class_student class_student
                  ON class_student.class_id = class_info.id
                 AND class_student.state = 1
                JOIN el_user student
                  ON student.user_id = class_student.user_id
                 AND student.del_flag = 1
                LEFT JOIN live_user_course_record attendance
                  ON attendance.course_id = course.course_id
                 AND attendance.user_id = class_student.user_id
                WHERE course.course_id = ?
                  AND course.del_flag = 1
                ORDER BY student.user_id
                """,
                (resultSet, rowNumber) -> new CrmStudentAttendanceData(
                        resultSet.getString("phone"),
                        resultSet.getObject("join_time", Long.class),
                        resultSet.getObject("leave_time", Long.class),
                        resultSet.getInt("is_attendance"),
                        resultSet.getInt("is_late"),
                        resultSet.getInt("is_leave_early")
                ),
                courseId
        );
    }

    @Override
    public boolean existsActiveRecording(String courseId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM live_video_record
                WHERE course_id = ?
                  AND status IN (-2, 0, 1)
                """,
                Integer.class,
                courseId
        );
        return count != null && count > 0;
    }

    @Override
    public List<String> findClosedCourseIds(long startTime, int limit) {
        return jdbcTemplate.query(
                """
                SELECT course_id
                FROM live_course
                WHERE status = 3
                  AND del_flag = 1
                  AND COALESCE(end_time, course_time) >= ?
                ORDER BY COALESCE(end_time, course_time), course_id
                LIMIT ?
                """,
                (resultSet, rowNumber) -> resultSet.getString("course_id"),
                startTime,
                limit
        );
    }
}
