package com.ai4.externalcrm.dto;

/**
 * CRM 创建班级时需要的课包及直播课统计数据。
 */
public record CrmCoursePackageStatResponse(
        Long packageId,
        String packageName,
        Long liveCourseCount
) {
}
