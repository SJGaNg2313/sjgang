package com.ai4.livecourse.dto;

/**
 * 直播课录像信息。
 */
public record LiveCourseVideoResponse(
        String videoName,
        String videoUrl
) {
}
