package com.ai4.task.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 项目领取任务详情响应 */
public record ProjectTaskResponse(
        Long id,
        Long projectId,
        List<TaskTypeRequest> taskTypes,
        String companyPositioning,
        String companyCard,
        List<String> companyNames,
        String taskName,
        String taskImage,
        String taskObjective,
        List<String> taskKeywords,
        String submissionIcon,
        String submissionName,
        String submissionDescription,
        List<ProjectTaskPartnerResponse> partners,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
