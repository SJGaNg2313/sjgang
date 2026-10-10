package com.ai4.livecourse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 直播课分页列表项。 */
public record LiveCourseListItemResponse(
        String courseId,
        String roomId,
        String className,
        String projectName,
        String teacherName,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime courseTime,

        Integer courseDuration,
        Integer status,
        Long attendanceCount,
        Long lateCount
) {
}
