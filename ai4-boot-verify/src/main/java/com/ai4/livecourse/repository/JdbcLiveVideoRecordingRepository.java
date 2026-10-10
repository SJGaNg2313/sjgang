package com.ai4.livecourse.repository;

import com.ai4.livecourse.model.LiveCourseRecordingData;
import com.ai4.livecourse.model.LiveCourseVideoData;
import com.ai4.livecourse.model.LiveVideoRecording;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 使用 JdbcTemplate 持久化声网云端录制任务。 */
@Repository
public class JdbcLiveVideoRecordingRepository implements LiveVideoRecordingRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLiveVideoRecordingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<LiveCourseRecordingData> findCourse(String courseId) {
        return jdbcTemplate.query(
                """
                SELECT
                    course.course_id,
                    course.room_id,
                    course.teacher_id,
                    COALESCE(project.project_name, course.course_name, '') AS room_name,
                    course.course_time,
                    course.course_duration,
                    course.close_delay,
                    course.room_type,
                    course.record,
                    course.status
                FROM live_course course
                LEFT JOIN ai4_class_live_course relation
                  ON relation.course_id = course.course_id
                LEFT JOIN ai4_project project
                  ON project.id = relation.project_id
                 AND project.del_flag = 1
                WHERE course.course_id = ?
                  AND course.del_flag = 1
                LIMIT 1
                """,
                (resultSet, rowNumber) -> new LiveCourseRecordingData(
                        resultSet.getString("course_id"),
                        resultSet.getString("room_id"),
                        resultSet.getString("teacher_id"),
                        resultSet.getString("room_name"),
                        resultSet.getLong("course_time"),
                        resultSet.getInt("course_duration"),
                        resultSet.getInt("close_delay"),
                        resultSet.getInt("room_type"),
                        resultSet.getInt("record"),
                        resultSet.getInt("status")
                ),
                courseId
        ).stream().findFirst();
    }

    @Override
    public Optional<LiveVideoRecording> findActiveByCourseId(String courseId) {
        return queryRecording(
                """
                SELECT recording.*, course.room_id
                FROM live_video_record recording
                JOIN live_course course ON course.course_id = recording.course_id
                WHERE recording.course_id = ?
                  AND recording.status IN (-2, 0, 1)
                ORDER BY recording.create_time DESC
                LIMIT 1
                """,
                courseId
        );
    }

    @Override
    public Optional<LiveVideoRecording> findActiveByRoomIdAndSecretKey(
            String roomId,
            String secretKey
    ) {
        return queryRecording(
                """
                SELECT recording.*, course.room_id
                FROM live_video_record recording
                JOIN live_course course ON course.course_id = recording.course_id
                WHERE course.room_id = ?
                  AND recording.secret_key = ?
                  AND recording.status IN (-2, 0, 1)
                ORDER BY recording.create_time DESC
                LIMIT 1
                """,
                roomId,
                secretKey
        );
    }

    @Override
    public Optional<LiveVideoRecording> findBySid(String sid) {
        return queryRecording(
                """
                SELECT recording.*, course.room_id
                FROM live_video_record recording
                JOIN live_course course ON course.course_id = recording.course_id
                WHERE recording.sid = ?
                LIMIT 1
                """,
                sid
        );
    }

    @Override
    public List<String> findClosedCourseIdsWithActiveRecording(int limit) {
        return jdbcTemplate.query(
                """
                SELECT DISTINCT recording.course_id
                FROM live_video_record recording
                JOIN live_course course ON course.course_id = recording.course_id
                WHERE course.status = 3
                  AND recording.status IN (-2, 0, 1)
                ORDER BY recording.course_id
                LIMIT ?
                """,
                (resultSet, rowNumber) -> resultSet.getString("course_id"),
                limit
        );
    }

    @Override
    public void save(LiveVideoRecording recording) {
        jdbcTemplate.update(
                """
                INSERT INTO live_video_record(
                    course_id, resource_id, sid, status, file_list,
                    create_time, user_id, role_type, uid, url,
                    max_recording_hour, server, secret_key
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                recording.courseId(),
                recording.resourceId(),
                recording.sid(),
                recording.status(),
                recording.fileList(),
                recording.createTime(),
                recording.userId(),
                recording.roleType(),
                recording.uid(),
                recording.url(),
                recording.maxRecordingHour(),
                recording.server(),
                recording.secretKey()
        );
    }

    @Override
    public void updateRunningState(String sid, int status) {
        jdbcTemplate.update(
                "UPDATE live_video_record SET status = ?, server = 1 WHERE sid = ?",
                status,
                sid
        );
    }

    @Override
    public void markServerReady(String sid) {
        jdbcTemplate.update(
                "UPDATE live_video_record SET server = 1 WHERE sid = ? AND server = 0",
                sid
        );
    }

    @Override
    public void markStopped(String sid, String fileList) {
        jdbcTemplate.update(
                "UPDATE live_video_record SET status = 2, server = 2, file_list = ? WHERE sid = ?",
                fileList,
                sid
        );
    }

    @Override
    public void markFailed(String sid, String fileList) {
        jdbcTemplate.update(
                "UPDATE live_video_record SET status = -1, server = -1, file_list = ? WHERE sid = ?",
                fileList,
                sid
        );
    }

    @Override
    public Optional<LiveCourseVideoData> findCourseVideo(String courseId) {
        return jdbcTemplate.query(
                """
                SELECT course_id, file_list, m3u8, start_time
                FROM live_course_video
                WHERE course_id = ?
                LIMIT 1
                """,
                (resultSet, rowNumber) -> new LiveCourseVideoData(
                        resultSet.getString("course_id"),
                        resultSet.getString("file_list"),
                        resultSet.getString("m3u8"),
                        resultSet.getObject("start_time", Long.class)
                ),
                courseId
        ).stream().findFirst();
    }

    @Override
    public void saveCourseVideo(LiveCourseVideoData video) {
        jdbcTemplate.update(
                """
                INSERT INTO live_course_video(course_id, file_list, m3u8, start_time)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    file_list = VALUES(file_list),
                    m3u8 = VALUES(m3u8),
                    start_time = VALUES(start_time)
                """,
                video.courseId(),
                video.fileList(),
                video.m3u8(),
                video.startTime()
        );
    }

    private Optional<LiveVideoRecording> queryRecording(String sql, Object... parameters) {
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new LiveVideoRecording(
                        resultSet.getString("course_id"),
                        resultSet.getString("resource_id"),
                        resultSet.getString("sid"),
                        resultSet.getInt("status"),
                        resultSet.getString("file_list"),
                        resultSet.getLong("create_time"),
                        resultSet.getString("user_id"),
                        resultSet.getInt("role_type"),
                        resultSet.getString("uid"),
                        resultSet.getString("url"),
                        resultSet.getInt("max_recording_hour"),
                        resultSet.getInt("server"),
                        resultSet.getString("secret_key"),
                        resultSet.getString("room_id")
                ),
                parameters
        ).stream().findFirst();
    }
}
