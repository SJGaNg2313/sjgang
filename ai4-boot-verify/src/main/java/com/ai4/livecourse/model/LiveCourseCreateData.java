package com.ai4.livecourse.model;

/** 写入直播课主表所需的数据。 */
public record LiveCourseCreateData(
        String courseId,
        long courseTime,
        int courseDuration,
        String teacherId,
        int closeDelay,
        int systemDefault,
        int record,
        String roomId,
        int roomType
) {
}
