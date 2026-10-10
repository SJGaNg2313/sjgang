package com.ai4.livecourse.dto;

/** 新增直播课结果。 */
public record CreateLiveCourseResponse(
        String courseId,
        String roomId
) {
}
