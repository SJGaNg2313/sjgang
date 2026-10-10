package com.ai4.livecourse.repository;

import com.ai4.livecourse.model.AgoraEventCourseData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 使用 JdbcTemplate 持久化声网课堂事件。 */
@Repository
public class JdbcLiveCourseEventRepository implements LiveCourseEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLiveCourseEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AgoraEventCourseData> findCoursesNeedingEventSync(
            long earliestCloseTime,
            long latestStartTime
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    course.course_id,
                    course.room_id,
                    course.teacher_id,
                    course.course_time,
                    course.course_duration,
                    course.close_delay
                FROM live_course course
                WHERE course.del_flag = 1
                  AND course.status IN (0, 1, 2)
                  AND course.course_time <= ?
                  AND course.course_time
                      + (course.course_duration + course.close_delay) * 60000 >= ?
                ORDER BY course.course_time, course.course_id
                """,
                (resultSet, rowNumber) -> new AgoraEventCourseData(
                        resultSet.getString("course_id"),
                        resultSet.getString("room_id"),
                        resultSet.getString("teacher_id"),
                        resultSet.getLong("course_time"),
                        resultSet.getInt("course_duration"),
                        resultSet.getInt("close_delay")
                ),
                latestStartTime,
                earliestCloseTime
        );
    }

    @Override
    public boolean isAssignedTeacher(String courseId, String teacherId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM live_course
                WHERE course_id = ?
                  AND teacher_id = ?
                  AND del_flag = 1
                """,
                Integer.class,
                courseId,
                teacherId
        );
        return count != null && count > 0;
    }

    @Override
    public boolean isActiveStudent(String courseId, String studentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM ai4_class_live_course course_relation
                JOIN ai4_class_student class_student
                  ON class_student.class_id = course_relation.class_id
                 AND class_student.state = 1
                WHERE course_relation.course_id = ?
                  AND class_student.user_id = ?
                """,
                Integer.class,
                courseId,
                studentId
        );
        return count != null && count > 0;
    }

    @Override
    public void updateCourseState(String courseId, int state, Long eventTime) {
        if (state == 1) {
            jdbcTemplate.update(
                    """
                    UPDATE live_course
                    SET status = CASE WHEN status < 1 THEN 1 ELSE status END,
                        start_time = COALESCE(start_time, ?)
                    WHERE course_id = ?
                      AND del_flag = 1
                      AND status < 3
                    """,
                    eventTime,
                    courseId
            );
            return;
        }
        if (state == 2) {
            jdbcTemplate.update(
                    """
                    UPDATE live_course
                    SET status = CASE WHEN status < 2 THEN 2 ELSE status END
                    WHERE course_id = ?
                      AND del_flag = 1
                      AND status < 3
                    """,
                    courseId
            );
            return;
        }
        if (state == 3) {
            jdbcTemplate.update(
                    """
                    UPDATE live_course
                    SET status = 3,
                        end_time = COALESCE(?, end_time)
                    WHERE course_id = ?
                      AND del_flag = 1
                    """,
                    eventTime,
                    courseId
            );
        }
    }

    @Override
    public void upsertOnlineUser(
            String courseId,
            String userId,
            int role,
            long updateTime
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO live_course_room_user(
                    course_id, user_id, role_type, create_time, update_time
                ) VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    role_type = VALUES(role_type),
                    update_time = GREATEST(update_time, VALUES(update_time))
                """,
                courseId,
                userId,
                role,
                updateTime,
                updateTime
        );
    }

    @Override
    public void deleteOnlineUser(String courseId, String userId) {
        jdbcTemplate.update(
                "DELETE FROM live_course_room_user WHERE course_id = ? AND user_id = ?",
                courseId,
                userId
        );
    }

    @Override
    public void recordStudentJoined(
            String recordId,
            String courseId,
            String studentId,
            long joinTime,
            String streamUuid,
            long courseTime
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO live_user_course_record(
                    id, course_id, user_id, join_time, stream_uuid,
                    be_duty, be_late, lack, leave_early
                ) VALUES (?, ?, ?, ?, ?, 1, ?, 0, 0)
                ON DUPLICATE KEY UPDATE
                    join_time = IF(
                        join_time IS NULL OR join_time > VALUES(join_time),
                        VALUES(join_time),
                        join_time
                    ),
                    stream_uuid = IF(
                        stream_uuid IS NULL OR stream_uuid = '',
                        VALUES(stream_uuid),
                        stream_uuid
                    ),
                    be_duty = 1,
                    be_late = IF(join_time > ?, 1, 0),
                    lack = 0,
                    leave_early = 0
                """,
                recordId,
                courseId,
                studentId,
                joinTime,
                streamUuid,
                joinTime > courseTime ? 1 : 0,
                courseTime
        );
    }

    @Override
    public void recordTeacherJoined(
            String recordId,
            String courseId,
            String teacherId,
            long joinTime,
            String streamUuid,
            long courseTime
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO live_teach_course_record(
                    id, course_id, teach_id, join_time, stream_uuid,
                    be_duty, be_late, lack, leave_early
                ) VALUES (?, ?, ?, ?, ?, 1, ?, 0, 0)
                ON DUPLICATE KEY UPDATE
                    teach_id = VALUES(teach_id),
                    join_time = IF(
                        join_time IS NULL OR join_time > VALUES(join_time),
                        VALUES(join_time),
                        join_time
                    ),
                    stream_uuid = IF(
                        stream_uuid IS NULL OR stream_uuid = '',
                        VALUES(stream_uuid),
                        stream_uuid
                    ),
                    be_duty = 1,
                    be_late = IF(join_time > ?, 1, 0),
                    lack = 0,
                    leave_early = 0
                """,
                recordId,
                courseId,
                teacherId,
                joinTime,
                streamUuid,
                joinTime > courseTime ? 1 : 0,
                courseTime
        );
    }

    @Override
    public void recordStudentLeft(
            String recordId,
            String courseId,
            String studentId,
            long leaveTime,
            String streamUuid,
            long scheduledEndTime
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO live_user_course_record(
                    id, course_id, user_id, leave_time, stream_uuid, leave_early
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    leave_time = IF(
                        leave_time IS NULL OR leave_time < VALUES(leave_time),
                        VALUES(leave_time),
                        leave_time
                    ),
                    stream_uuid = IF(
                        stream_uuid IS NULL OR stream_uuid = '',
                        VALUES(stream_uuid),
                        stream_uuid
                    ),
                    leave_early = IF(leave_time < ?, 1, 0)
                """,
                recordId,
                courseId,
                studentId,
                leaveTime,
                streamUuid,
                leaveTime < scheduledEndTime ? 1 : 0,
                scheduledEndTime
        );
    }

    @Override
    public void recordTeacherLeft(
            String recordId,
            String courseId,
            String teacherId,
            long leaveTime,
            String streamUuid,
            long scheduledEndTime
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO live_teach_course_record(
                    id, course_id, teach_id, leave_time, stream_uuid, leave_early
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    teach_id = VALUES(teach_id),
                    leave_time = IF(
                        leave_time IS NULL OR leave_time < VALUES(leave_time),
                        VALUES(leave_time),
                        leave_time
                    ),
                    stream_uuid = IF(
                        stream_uuid IS NULL OR stream_uuid = '',
                        VALUES(stream_uuid),
                        stream_uuid
                    ),
                    leave_early = IF(leave_time < ?, 1, 0)
                """,
                recordId,
                courseId,
                teacherId,
                leaveTime,
                streamUuid,
                leaveTime < scheduledEndTime ? 1 : 0,
                scheduledEndTime
        );
    }

    @Override
    public List<String> findActiveStudentIds(String courseId) {
        return jdbcTemplate.query(
                """
                SELECT class_student.user_id
                FROM ai4_class_live_course course_relation
                JOIN ai4_class_student class_student
                  ON class_student.class_id = course_relation.class_id
                 AND class_student.state = 1
                WHERE course_relation.course_id = ?
                ORDER BY class_student.user_id
                """,
                (resultSet, rowNumber) -> resultSet.getString("user_id"),
                courseId
        );
    }

    @Override
    public void ensureStudentAttendanceRecord(
            String recordId,
            String courseId,
            String studentId
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO live_user_course_record(
                    id, course_id, user_id, lack
                ) VALUES (?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE
                    lack = IF(be_duty = 1, 0, 1)
                """,
                recordId,
                courseId,
                studentId
        );
    }

    @Override
    public void ensureTeacherAttendanceRecord(
            String recordId,
            String courseId,
            String teacherId
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO live_teach_course_record(
                    id, course_id, teach_id, lack
                ) VALUES (?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE
                    teach_id = VALUES(teach_id),
                    lack = IF(be_duty = 1, 0, 1)
                """,
                recordId,
                courseId,
                teacherId
        );
    }

    @Override
    public void finalizeAttendance(
            String courseId,
            long scheduledEndTime,
            long closeTime
    ) {
        jdbcTemplate.update(
                """
                UPDATE live_user_course_record attendance
                JOIN live_course_room_user online_user
                  ON online_user.course_id = attendance.course_id
                 AND online_user.user_id = attendance.user_id
                 AND online_user.role_type = 2
                SET attendance.leave_time = ?,
                    attendance.leave_early = 0
                WHERE attendance.course_id = ?
                """,
                closeTime,
                courseId
        );
        jdbcTemplate.update(
                """
                UPDATE live_teach_course_record attendance
                JOIN live_course_room_user online_user
                  ON online_user.course_id = attendance.course_id
                 AND online_user.user_id = attendance.teach_id
                 AND online_user.role_type = 1
                SET attendance.leave_time = ?,
                    attendance.leave_early = 0
                WHERE attendance.course_id = ?
                """,
                closeTime,
                courseId
        );
        jdbcTemplate.update(
                """
                UPDATE live_user_course_record
                SET lack = IF(be_duty = 1, 0, 1),
                    leave_early = CASE
                        WHEN be_duty = 0 THEN 0
                        WHEN leave_time IS NULL THEN 0
                        WHEN leave_time < ? THEN 1
                        ELSE 0
                    END
                WHERE course_id = ?
                """,
                scheduledEndTime,
                courseId
        );
        jdbcTemplate.update(
                """
                UPDATE live_teach_course_record
                SET lack = IF(be_duty = 1, 0, 1),
                    leave_early = CASE
                        WHEN be_duty = 0 THEN 0
                        WHEN leave_time IS NULL THEN 0
                        WHEN leave_time < ? THEN 1
                        ELSE 0
                    END
                WHERE course_id = ?
                """,
                scheduledEndTime,
                courseId
        );
        jdbcTemplate.update(
                "DELETE FROM live_course_room_user WHERE course_id = ?",
                courseId
        );
    }
}
