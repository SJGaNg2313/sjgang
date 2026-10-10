package com.ai4.project.model;

import java.time.LocalDateTime;

/** 项目数据库模型 */
public record Project(
        Long id,
        Long packageId,
        String projectName,
        String projectIntro,
        String projectCode,
        Integer sort,
        Integer delFlag,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
