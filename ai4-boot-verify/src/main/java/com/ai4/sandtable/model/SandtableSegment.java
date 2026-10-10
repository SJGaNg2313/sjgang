package com.ai4.sandtable.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 沙盘视频分段模型。 */
public record SandtableSegment(
        Long id,
        Long sandtableId,
        BigDecimal segmentTime,
        String segmentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
