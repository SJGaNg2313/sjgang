package com.ai4.sandtable.dto;

import java.math.BigDecimal;

/** 沙盘属性响应。 */
public record SandtableAttributeResponse(
        Long id,
        String attributeName,
        String icon,
        BigDecimal target,
        BigDecimal initial
) {
}
