package com.ai4.externalcrm.model;

/** 直播课结束时组装 CRM 回调的学生考勤记录。 */
public record CrmStudentAttendanceData(
        String phone,
        Long joinTime,
        Long leaveTime,
        int attendance,
        int late,
        int leaveEarly
) {
}
