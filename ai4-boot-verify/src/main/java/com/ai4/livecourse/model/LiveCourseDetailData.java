package com.ai4.livecourse.model;

/**
 * 计算直播课详情所需的基础数据。
 */
public record LiveCourseDetailData(
        String courseId,
        String roomId,
        String className,
        String teacherName,
        Long courseTime,
        Integer status,
        Long startTime,
        Long endTime
) {
}
