package com.ai4.task.model;

import java.time.LocalDateTime;

/** 项目领取任务数据库模型 */
public record ProjectTask(
        Long id,
        Long projectId,
        String taskTypes,
        String companyPositioning,
        String companyCard,
        String companyNames,
        String taskName,
        String taskImage,
        String taskObjective,
        String taskKeywords,
        String submissionIcon,
        String submissionName,
        String submissionDescription,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
