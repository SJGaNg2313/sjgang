package com.ai4.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 新增或修改题目时提交的选项。 */
public record QuestionOptionRequest(
        @NotBlank(message = "选项文本不能为空")
        String optionText,

        @NotBlank(message = "影响类型不能为空")
        String impactType,

        String afterGuide,

        @NotEmpty(message = "属性影响不能为空")
        List<@Valid QuestionOptionAttributeRequest> attributes
) {
}
