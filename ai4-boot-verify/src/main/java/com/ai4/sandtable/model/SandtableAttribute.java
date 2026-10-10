package com.ai4.sandtable.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 沙盘属性模型。 */
public record SandtableAttribute(
        Long id,
        Long sandtableId,
        String attributeName,
        String icon,
        BigDecimal target,
        BigDecimal initial,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
