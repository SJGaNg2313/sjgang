package com.ai4.livecourse.model;

/** 启动或停止云端录制所需的直播课数据。 */
public record LiveCourseRecordingData(
        String courseId,
        String roomId,
        String teacherId,
        String roomName,
        long courseTime,
        int courseDuration,
        int closeDelay,
        int roomType,
        int record,
        int status
) {
}
