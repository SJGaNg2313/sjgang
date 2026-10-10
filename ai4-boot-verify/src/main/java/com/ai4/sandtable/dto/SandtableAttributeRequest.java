package com.ai4.sandtable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 沙盘属性请求；新增属性时id为空，修改时已有属性携带id。 */
public record SandtableAttributeRequest(
        Long id,

        @NotBlank(message = "属性名称不能为空")
        @Size(max = 10, message = "属性名称不能超过10个字符")
        String attributeName,

        @Size(max = 255, message = "属性图标不能超过255个字符")
        String icon,

        BigDecimal target,

        @NotNull(message = "初始值不能为空")
        BigDecimal initial
) {
}
