package com.ai4.externalcrm.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

/** CRM 班级写入数据库所需的数据。 */
public record CrmClassCreateData(
        String classId,
        Integer crmClassId,
        String className,
        LocalTime startTime,
        LocalTime endTime,
        Long packageId,
        String teacherId,
        Integer classType,
        Integer roomType,
        Integer record,
        Integer courseRule,
        LocalDateTime startClassTime,
        Integer classStatus,
        Integer closeDelay,
        Integer unlockInterval
) {
}
