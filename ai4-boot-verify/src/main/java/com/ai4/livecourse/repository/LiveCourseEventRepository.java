package com.ai4.livecourse.repository;

import com.ai4.livecourse.model.AgoraEventCourseData;

import java.util.List;

/** 声网课堂事件同步使用的数据访问接口。 */
public interface LiveCourseEventRepository {

    List<AgoraEventCourseData> findCoursesNeedingEventSync(
            long earliestCloseTime,
            long latestStartTime
    );

    boolean isAssignedTeacher(String courseId, String teacherId);

    boolean isActiveStudent(String courseId, String studentId);

    void updateCourseState(String courseId, int state, Long eventTime);

    void upsertOnlineUser(
            String courseId,
            String userId,
            int role,
            long updateTime
    );

    void deleteOnlineUser(String courseId, String userId);

    void recordStudentJoined(
            String recordId,
            String courseId,
            String studentId,
            long joinTime,
            String streamUuid,
            long courseTime
    );

    void recordTeacherJoined(
            String recordId,
            String courseId,
            String teacherId,
            long joinTime,
            String streamUuid,
            long courseTime
    );

    void recordStudentLeft(
            String recordId,
            String courseId,
            String studentId,
            long leaveTime,
            String streamUuid,
            long scheduledEndTime
    );

    void recordTeacherLeft(
            String recordId,
            String courseId,
            String teacherId,
            long leaveTime,
            String streamUuid,
            long scheduledEndTime
    );

    List<String> findActiveStudentIds(String courseId);

    void ensureStudentAttendanceRecord(
            String recordId,
            String courseId,
            String studentId
    );

    void ensureTeacherAttendanceRecord(
            String recordId,
            String courseId,
            String teacherId
    );

    void finalizeAttendance(
            String courseId,
            long scheduledEndTime,
            long closeTime
    );
}
