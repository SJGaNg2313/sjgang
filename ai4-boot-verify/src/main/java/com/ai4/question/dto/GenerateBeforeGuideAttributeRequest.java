package com.ai4.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 生成题前引导时提交的单个属性影响。 */
public record GenerateBeforeGuideAttributeRequest(
        @NotNull(message = "属性ID不能为空")
        Long attributeId,

        @NotBlank(message = "属性影响不能为空")
        String attributeEffect
) {
}
