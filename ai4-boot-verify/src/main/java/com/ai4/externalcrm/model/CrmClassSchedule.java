package com.ai4.externalcrm.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** 已保存班级的自动排课配置。 */
public record CrmClassSchedule(
        String classId,
        Long packageId,
        LocalTime startTime,
        LocalTime endTime,
        LocalDateTime startClassTime,
        Integer courseRule,
        String teacherId,
        Integer roomType,
        Integer record,
        Integer closeDelay,
        Integer classStatus
) {
}
