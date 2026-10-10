package com.ai4.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 一次生成题前引导和每个选项的选后引导。 */
public record GenerateQuestionGuidesRequest(
        @NotNull(message = "项目ID不能为空")
        Long projectId,

        String imageDesc,

        @NotBlank(message = "题目文本不能为空")
        String questionText,

        @NotEmpty(message = "选项不能为空")
        @Size(min = 2, max = 4, message = "选项数量为2至4个")
        List<@Valid GenerateBeforeGuideOptionRequest> options
) {
}
