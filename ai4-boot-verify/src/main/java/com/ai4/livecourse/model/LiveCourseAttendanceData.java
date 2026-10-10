package com.ai4.livecourse.model;

/**
 * 直播课出勤统计数据。
 */
public record LiveCourseAttendanceData(
        Long attendanceCount,
        Long absenceCount,
        Long lateCount,
        Long earlyLeaveCount
) {
}
