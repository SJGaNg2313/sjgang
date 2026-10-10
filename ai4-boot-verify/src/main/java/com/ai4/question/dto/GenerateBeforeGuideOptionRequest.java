package com.ai4.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/** 生成题前引导时提交的选项信息。 */
public record GenerateBeforeGuideOptionRequest(
        @NotBlank(message = "选项文本不能为空")
        String optionText,

        @NotBlank(message = "影响类型不能为空")
        @Pattern(regexp = "GOOD|BAD", message = "影响类型只能是GOOD或BAD")
        String impactType,

        @Valid
        @NotEmpty(message = "属性影响不能为空")
        List<GenerateBeforeGuideAttributeRequest> attributes
) {
}
