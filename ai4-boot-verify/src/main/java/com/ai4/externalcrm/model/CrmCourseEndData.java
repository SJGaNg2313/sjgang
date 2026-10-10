package com.ai4.externalcrm.model;

/** 组装 CRM 班级结束回调所需的直播课数据。 */
public record CrmCourseEndData(
        String courseId,
        int crmClassId,
        long courseTime,
        int courseDuration,
        int status,
        int record,
        String m3u8UrlsJson
) {
}
