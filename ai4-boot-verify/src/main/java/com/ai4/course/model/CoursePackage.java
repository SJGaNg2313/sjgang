package com.ai4.course.model;

import java.time.LocalDateTime;

/** 课包数据库模型。 */
public record CoursePackage(
        Long id,
        String packageName,
        Integer sortOrder,
        Integer delFlag,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
