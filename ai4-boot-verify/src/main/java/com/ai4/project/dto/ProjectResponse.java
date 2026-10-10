package com.ai4.project.dto;

import java.time.LocalDateTime;

/** 项目列表及详情响应 */
public record ProjectResponse(
        Long id,
        Long packageId,
        String projectName,
        String projectIntro,
        String projectCode,
        Integer sort,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
