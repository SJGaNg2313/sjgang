package com.ai4.sandtable.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 项目沙盘完整配置响应。 */
public record ProjectSandtableResponse(
        Long id,
        Long projectId,
        String videoUrl,
        String trendUrl,
        List<SandtableSegmentResponse> segments,
        List<SandtableAttributeResponse> attributes,
        List<SandtableGainResponse> gains,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
