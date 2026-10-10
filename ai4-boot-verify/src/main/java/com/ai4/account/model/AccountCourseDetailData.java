package com.ai4.account.model;

/**
 * 查询学员及其最近直播课所需的数据。
 */
public record AccountCourseDetailData(
        String userId,
        String phone,
        String className,
        String courseId,
        String projectName,
        String teacherName,
        Long courseTime,
        Integer status,
        Long startTime,
        Long endTime,
        Boolean attended
) {
}
