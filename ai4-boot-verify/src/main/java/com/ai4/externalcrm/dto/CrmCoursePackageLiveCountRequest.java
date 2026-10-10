package com.ai4.externalcrm.dto;

/** CRM 课包直播课数量同步请求。 */
public record CrmCoursePackageLiveCountRequest(
        long packageId,
        long liveLessonCount
) {
}
