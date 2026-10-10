package com.ai4.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 选项对单个沙盘属性的影响配置。 */
public record QuestionOptionAttributeRequest(
        @NotNull(message = "属性ID不能为空")
        Long attributeId,

        @NotBlank(message = "属性影响不能为空")
        String attributeEffect
) {
}