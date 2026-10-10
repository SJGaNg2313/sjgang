package com.ai4.livecourse.model;

/** 拉取声网课堂事件所需的直播课数据。 */
public record AgoraEventCourseData(
        String courseId,
        String roomId,
        String teacherId,
        long courseTime,
        int courseDuration,
        int closeDelay
) {

    public long scheduledEndTime() {
        return courseTime + courseDuration * 60_000L;
    }
}
