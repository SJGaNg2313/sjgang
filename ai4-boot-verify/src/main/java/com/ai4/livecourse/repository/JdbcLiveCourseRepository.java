package com.ai4.livecourse.repository;

import com.ai4.livecourse.dto.LiveCourseListItemResponse;
import com.ai4.livecourse.dto.LiveCourseDetailStudentResponse;
import com.ai4.livecourse.dto.LiveCoursePageQuery;
import com.ai4.livecourse.dto.StudentSubmissionFileResponse;
import com.ai4.livecourse.model.LiveCourseAttendanceData;
import com.ai4.livecourse.model.LiveCourseCreateData;
import com.ai4.livecourse.model.LiveCourseDetailData;
import com.ai4.livecourse.model.AgoraRoomProvisionData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 使用 JdbcTemplate 查询直播课列表。 */
@Repository
public class JdbcLiveCourseRepository implements LiveCourseRepository {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private static final String FROM_SQL = """
            FROM live_course course
            JOIN ai4_class_live_course relation
              ON relation.course_id = course.course_id
            JOIN ai4_class class_info
              ON class_info.id = relation.class_id
             AND class_info.del_flag = 1
            JOIN ai4_project project
              ON project.id = relation.project_id
             AND project.del_flag = 1
            LEFT JOIN live_teach teacher
              ON teacher.id = course.teacher_id
             AND teacher.del_flag = 1
            LEFT JOIN (
                SELECT
                    course_id,
                    SUM(CASE WHEN be_duty = 1 THEN 1 ELSE 0 END) AS attendance_count,
                    SUM(CASE WHEN be_late = 1 THEN 1 ELSE 0 END) AS late_count
                FROM live_user_course_record
                GROUP BY course_id
            ) attendance ON attendance.course_id = course.course_id
            WHERE course.del_flag = 1
            """;

    private static final String COUNT_FROM_SQL = """
            FROM live_course course
            JOIN ai4_class_live_course relation
              ON relation.course_id = course.course_id
            JOIN ai4_class class_info
              ON class_info.id = relation.class_id
             AND class_info.del_flag = 1
            JOIN ai4_project project
              ON project.id = relation.project_id
             AND project.del_flag = 1
            LEFT JOIN live_teach teacher
              ON teacher.id = course.teacher_id
             AND teacher.del_flag = 1
            WHERE course.del_flag = 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcLiveCourseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long count(LiveCoursePageQuery query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) ").append(COUNT_FROM_SQL);
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
    public List<LiveCourseListItemResponse> findPage(
            LiveCoursePageQuery query,
            int offset,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    course.course_id,
                    course.room_id,
                    class_info.class_name,
                    project.project_name,
                    COALESCE(NULLIF(teacher.nickname, ''), teacher.username, '') AS teacher_name,
                    course.course_time,
                    course.course_duration,
                    course.status,
                    COALESCE(attendance.attendance_count, 0) AS attendance_count,
                    COALESCE(attendance.late_count, 0) AS late_count
                """).append(FROM_SQL);
        List<Object> parameters = new ArrayList<>();
        appendFilters(sql, parameters, query);
        sql.append(" ORDER BY course.course_time DESC, course.course_id DESC LIMIT ? OFFSET ?");
        parameters.add(limit);
        parameters.add(offset);

        return jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> {
                    return new LiveCourseListItemResponse(
                            resultSet.getString("course_id"),
                            resultSet.getString("room_id"),
                            resultSet.getString("class_name"),
                            resultSet.getString("project_name"),
                            resultSet.getString("teacher_name"),
                            Instant.ofEpochMilli(resultSet.getLong("course_time"))
                                    .atZone(BUSINESS_ZONE)
                                    .toLocalDateTime(),
                            resultSet.getInt("course_duration"),
                            resultSet.getInt("status"),
                            resultSet.getLong("attendance_count"),
                            resultSet.getLong("late_count")
                    );
                },
                parameters.toArray()
        );
    }

    @Override
    public Optional<LiveCourseDetailData> findDetailById(String courseId) {
        return jdbcTemplate.query(
                """
                SELECT
                    course.course_id,
                    course.room_id,
                    class_info.class_name,
                    COALESCE(NULLIF(teacher.nickname, ''), teacher.username, '') AS teacher_name,
                    course.course_time,
                    course.status,
                    course.start_time,
                    course.end_time
                FROM live_course course
                JOIN ai4_class_live_course relation
                  ON relation.course_id = course.course_id
                JOIN ai4_class class_info
                  ON class_info.id = relation.class_id
                 AND class_info.del_flag = 1
                LEFT JOIN live_teach teacher
                  ON teacher.id = course.teacher_id
                 AND teacher.del_flag = 1
                WHERE course.course_id = ?
                  AND course.del_flag = 1
                LIMIT 1
                """,
                (resultSet, rowNumber) -> new LiveCourseDetailData(
                        resultSet.getString("course_id"),
                        resultSet.getString("room_id"),
                        resultSet.getString("class_name"),
                        resultSet.getString("teacher_name"),
                        resultSet.getLong("course_time"),
                        resultSet.getInt("status"),
                        resultSet.getObject("start_time", Long.class),
                        resultSet.getObject("end_time", Long.class)
                ),
                courseId
        ).stream().findFirst();
    }

    @Override
    public LiveCourseAttendanceData findAttendanceById(String courseId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT
                    COALESCE(SUM(CASE WHEN be_duty = 1 THEN 1 ELSE 0 END), 0) AS attendance_count,
                    COALESCE(SUM(CASE WHEN lack = 1 THEN 1 ELSE 0 END), 0) AS absence_count,
                    COALESCE(SUM(CASE WHEN be_late = 1 THEN 1 ELSE 0 END), 0) AS late_count,
                    COALESCE(SUM(CASE WHEN leave_early = 1 THEN 1 ELSE 0 END), 0) AS early_leave_count
                FROM live_user_course_record
                WHERE course_id = ?
                """,
                (resultSet, rowNumber) -> new LiveCourseAttendanceData(
                        resultSet.getLong("attendance_count"),
                        resultSet.getLong("absence_count"),
                        resultSet.getLong("late_count"),
                        resultSet.getLong("early_leave_count")
                ),
                courseId
        );
    }

    @Override
    public long countDetailStudents(String courseId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM live_course course
                JOIN ai4_class_live_course course_relation
                  ON course_relation.course_id = course.course_id
                JOIN ai4_class_student class_student
                  ON class_student.class_id = course_relation.class_id
                 AND class_student.state = 1
                JOIN el_user student
                  ON student.user_id = class_student.user_id
                 AND student.del_flag = 1
                WHERE course.course_id = ?
                  AND course.del_flag = 1
                """,
                Long.class,
                courseId
        );
        return count == null ? 0 : count;
    }

    @Override
    public List<LiveCourseDetailStudentResponse> findDetailStudents(
            String courseId,
            int offset,
            int limit
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    student.user_id,
                    COALESCE(student.phone, '') AS student_phone,
                    COALESCE(student.nick, '') AS student_name,
                    CASE
                        WHEN attendance_record.join_time IS NULL
                          OR attendance_record.join_time <= course.course_time THEN 0
                        ELSE (attendance_record.join_time - course.course_time) DIV 1000
                    END AS late_seconds,
                    attendance_record.stream_uuid AS probe_id
                FROM live_course course
                JOIN ai4_class_live_course course_relation
                  ON course_relation.course_id = course.course_id
                JOIN ai4_class_student class_student
                  ON class_student.class_id = course_relation.class_id
                 AND class_student.state = 1
                JOIN el_user student
                  ON student.user_id = class_student.user_id
                 AND student.del_flag = 1
                LEFT JOIN live_user_course_record attendance_record
                  ON attendance_record.course_id = course.course_id
                 AND attendance_record.user_id = class_student.user_id
                WHERE course.course_id = ?
                  AND course.del_flag = 1
                ORDER BY student.user_id
                LIMIT ? OFFSET ?
                """,
                (resultSet, rowNumber) -> new LiveCourseDetailStudentResponse(
                        resultSet.getString("user_id"),
                        resultSet.getString("student_phone"),
                        resultSet.getString("student_name"),
                        resultSet.getLong("late_seconds"),
                        resultSet.getString("probe_id")
                ),
                courseId,
                limit,
                offset
        );
    }

    @Override
    public List<StudentSubmissionFileResponse> findStudentSubmissionFiles(
            String courseId,
            String studentId
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    submission_node.name_snapshot,
                    file_resource.public_url
                FROM live_course course
                JOIN ai4_class_live_course course_relation
                  ON course_relation.course_id = course.course_id
                JOIN ai4_class_student class_student
                  ON class_student.class_id = course_relation.class_id
                 AND class_student.user_id = ?
                 AND class_student.state = 1
                JOIN el_user_founder_roundtable_record roundtable_record
                  ON roundtable_record.project_id = course_relation.project_id
                 AND roundtable_record.user_id = class_student.user_id
                 AND roundtable_record.del_flag = 1
                JOIN el_user_founder_roundtable_submission submission
                  ON submission.record_id = roundtable_record.id
                JOIN el_user_founder_roundtable_submission_node submission_node
                  ON submission_node.submission_id = submission.id
                LEFT JOIN file_resource file_resource
                  ON file_resource.file_id = submission_node.file_id
                 AND file_resource.status IN ('ACTIVE', 'RETAINED')
                WHERE course.course_id = ?
                  AND course.del_flag = 1
                  AND submission_node.node_type = 'FILE'
                ORDER BY submission.submitted_at DESC,
                         submission.id DESC,
                         submission_node.sort_order ASC,
                         submission_node.id ASC
                """,
                (resultSet, rowNumber) -> new StudentSubmissionFileResponse(
                        resultSet.getString("name_snapshot"),
                        resultSet.getString("public_url")
                ),
                studentId,
                courseId
        );
    }

    @Override
    public Optional<String> findVideoUrlsJson(String courseId) {
        return jdbcTemplate.query(
                "SELECT m3u8 FROM live_course_video WHERE course_id = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getString("m3u8"),
                courseId
        ).stream().filter(Objects::nonNull).findFirst();
    }

    @Override
    public List<AgoraRoomProvisionData> findRoomsNeedingProvisioning(
            long currentTime,
            long latestCourseTime
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    course.course_id,
                    course.room_id,
                    project.project_name AS room_name,
                    course.room_type,
                    course.course_time,
                    course.course_duration,
                    course.close_delay
                FROM live_course course
                JOIN ai4_class_live_course relation
                  ON relation.course_id = course.course_id
                JOIN ai4_project project
                  ON project.id = relation.project_id
                 AND project.del_flag = 1
                WHERE course.del_flag = 1
                  AND course.status = 0
                  AND course.course_time <= ?
                  AND course.course_time
                      + (course.course_duration + course.close_delay) * 60000 >= ?
                ORDER BY course.course_time, course.course_id
                """,
                (resultSet, rowNumber) -> new AgoraRoomProvisionData(
                        resultSet.getString("course_id"),
                        resultSet.getString("room_id"),
                        resultSet.getString("room_name"),
                        resultSet.getInt("room_type"),
                        resultSet.getLong("course_time"),
                        resultSet.getInt("course_duration"),
                        resultSet.getInt("close_delay")
                ),
                latestCourseTime,
                currentTime
        );
    }

    @Override
    public void save(LiveCourseCreateData course) {
        jdbcTemplate.update(
                """
                INSERT INTO live_course(
                    course_id,
                    course_time,
                    course_duration,
                    teacher_id,
                    status,
                    close_delay,
                    section,
                    system_default,
                    record,
                    del_flag,
                    is_temporary,
                    room_id,
                    room_type
                ) VALUES (?, ?, ?, ?, 0, ?, 1, ?, ?, 1, 0, ?, ?)
                """,
                course.courseId(),
                course.courseTime(),
                course.courseDuration(),
                course.teacherId(),
                course.closeDelay(),
                course.systemDefault(),
                course.record(),
                course.roomId(),
                course.roomType()
        );
    }

    @Override
    public void bindToClassAndProject(String classId, Long projectId, String courseId) {
        jdbcTemplate.update(
                """
                INSERT INTO ai4_class_live_course(class_id, project_id, course_id)
                VALUES (?, ?, ?)
                """,
                classId,
                projectId,
                courseId
        );
    }

    @Override
    public Optional<Long> findPackageIdByCourseId(String courseId) {
        return jdbcTemplate.query(
                """
                SELECT class_info.package_id
                FROM ai4_class_live_course relation
                JOIN ai4_class class_info
                  ON class_info.id = relation.class_id
                 AND class_info.del_flag = 1
                WHERE relation.course_id = ?
                LIMIT 1
                """,
                (resultSet, rowNumber) -> resultSet.getLong("package_id"),
                courseId
        ).stream().findFirst();
    }

    @Override
    public boolean existsActiveByClassAndProject(String classId, Long projectId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM ai4_class_live_course relation
                JOIN live_course course
                  ON course.course_id = relation.course_id
                 AND course.del_flag = 1
                WHERE relation.class_id = ?
                  AND relation.project_id = ?
                """,
                Integer.class,
                classId,
                projectId
        );
        return count != null && count > 0;
    }

    @Override
    public List<String> findUnstartedCourseIdsByClassId(String classId) {
        return jdbcTemplate.query(
                """
                SELECT course.course_id
                FROM live_course course
                JOIN ai4_class_live_course relation
                  ON relation.course_id = course.course_id
                WHERE relation.class_id = ?
                  AND course.status = 0
                  AND course.del_flag = 1
                """,
                (resultSet, rowNumber) -> resultSet.getString("course_id"),
                classId
        );
    }

    @Override
    public void softDeleteByIds(List<String> courseIds) {
        if (courseIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(courseIds.size(), "?"));
        jdbcTemplate.update(
                "UPDATE live_course SET del_flag = 2 "
                        + "WHERE status = 0 AND del_flag = 1 AND course_id IN (" + placeholders + ")",
                courseIds.toArray()
        );
    }

    @Override
    public void deleteRelationsByCourseIds(String classId, List<String> courseIds) {
        if (courseIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(courseIds.size(), "?"));
        List<Object> parameters = new ArrayList<>(courseIds.size() + 1);
        parameters.add(classId);
        parameters.addAll(courseIds);
        jdbcTemplate.update(
                "DELETE FROM ai4_class_live_course WHERE class_id = ? "
                        + "AND course_id IN (" + placeholders + ")",
                parameters.toArray()
        );
    }

    @Override
    public Optional<Integer> findStatusById(String courseId) {
        return jdbcTemplate.query(
                """
                SELECT status
                FROM live_course
                WHERE course_id = ?
                  AND del_flag = 1
                LIMIT 1
                """,
                (resultSet, rowNumber) -> resultSet.getInt("status"),
                courseId
        ).stream().findFirst();
    }

    @Override
    public int softDeleteUnstartedById(String courseId) {
        return jdbcTemplate.update(
                """
                UPDATE live_course
                SET del_flag = 2
                WHERE course_id = ?
                  AND status = 0
                  AND del_flag = 1
                """,
                courseId
        );
    }

    @Override
    public void deleteRelationByCourseId(String courseId) {
        jdbcTemplate.update(
                "DELETE FROM ai4_class_live_course WHERE course_id = ?",
                courseId
        );
    }

    private void appendFilters(
            StringBuilder sql,
            List<Object> parameters,
            LiveCoursePageQuery query
    ) {
        if (hasText(query.roomId())) {
            sql.append(" AND course.room_id LIKE ?");
            parameters.add("%" + query.roomId().trim() + "%");
        }
        if (hasText(query.className())) {
            sql.append(" AND class_info.class_name LIKE ?");
            parameters.add("%" + query.className().trim() + "%");
        }
        if (hasText(query.teacherName())) {
            sql.append(" AND COALESCE(NULLIF(teacher.nickname, ''), teacher.username, '') LIKE ?");
            parameters.add("%" + query.teacherName().trim() + "%");
        }
        if (query.startTime() != null) {
            sql.append(" AND course.course_time >= ?");
            parameters.add(toEpochMilli(query.startTime()));
        }
        if (query.endTime() != null) {
            sql.append(" AND course.course_time <= ?");
            parameters.add(toEpochMilli(query.endTime()));
        }
        if (query.status() != null) {
            sql.append(" AND course.status = ?");
            parameters.add(query.status());
        }
    }

    private long toEpochMilli(LocalDateTime time) {
        return time.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
