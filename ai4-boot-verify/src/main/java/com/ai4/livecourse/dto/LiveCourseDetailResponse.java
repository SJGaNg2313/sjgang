package com.ai4.livecourse.dto;

import com.ai4.common.response.PageResponse;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 直播课详情。
 */
public record LiveCourseDetailResponse(
        String courseId,
        String roomId,
        String className,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime courseTime,

        String teacherName,
        Long actualDurationSeconds,
        Long attendanceCount,
        Long absenceCount,
        Long lateCount,
        Long earlyLeaveCount,
        List<LiveCourseVideoResponse> videos,
        PageResponse<LiveCourseDetailStudentResponse> students
) {
}
