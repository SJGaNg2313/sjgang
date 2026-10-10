package com.ai4.sandtable.model;

import java.time.LocalDateTime;

/** 沙盘预计学习收获模型。 */
public record SandtableGain(
        Long id,
        Long sandtableId,
        String icon,
        String gain,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
