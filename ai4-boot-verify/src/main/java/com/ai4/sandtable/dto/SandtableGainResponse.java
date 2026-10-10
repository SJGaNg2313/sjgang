package com.ai4.sandtable.dto;

/** 沙盘预计学习收获响应。 */
public record SandtableGainResponse(
        Long id,
        String icon,
        String gain
) {
}
