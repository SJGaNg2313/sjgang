package com.ai4.sandtable.model;

import java.time.LocalDateTime;

/** 项目沙盘主表模型。 */
public record ProjectSandtable(
        Long id,
        Long projectId,
        String videoUrl,
        String trendUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
