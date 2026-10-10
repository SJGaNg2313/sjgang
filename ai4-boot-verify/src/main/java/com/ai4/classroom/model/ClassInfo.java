package com.ai4.classroom.model;

import java.time.LocalTime;

/** 创建直播课所需的有效班级信息。 */
public record ClassInfo(
        LocalTime startTime,
        Long packageId,
        String teacherId,
        Integer roomType,
        Integer record,
        Integer closeDelay
) {
}
