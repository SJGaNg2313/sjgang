package com.ai4.livecourse.model;

/** 直播课最终录像地址。 */
public record LiveCourseVideoData(
        String courseId,
        String fileList,
        String m3u8,
        Long startTime
) {
}
