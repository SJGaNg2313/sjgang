package com.ai4.sandtable.dto;

import java.math.BigDecimal;

/** 沙盘视频分段响应。 */
public record SandtableSegmentResponse(
        Long id,
        BigDecimal segmentTime,
        String segmentName
) {
}
