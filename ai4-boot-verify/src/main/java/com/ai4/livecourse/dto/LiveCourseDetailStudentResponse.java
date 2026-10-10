package com.ai4.livecourse.dto;

/**
 * 直播课详情中的学员记录。
 */
public record LiveCourseDetailStudentResponse(
        String studentId,
        String studentPhone,
        String studentName,
        Long lateSeconds,
        String probeId
) {
}
