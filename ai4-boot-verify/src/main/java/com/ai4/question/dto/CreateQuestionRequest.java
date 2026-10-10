package com.ai4.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 新增项目题目请求。 */
public record CreateQuestionRequest(
        @NotNull(message = "项目ID不能为空")
        Long projectId,

        @NotBlank(message = "题目图片不能为空")
        String imageUrl,

        String imageDesc,

        @NotBlank(message = "题目文本不能为空")
        String questionText,

        @NotNull(message = "月份不能为空")
        @Min(value = 1, message = "月份不能小于1")
        Integer month,

        String beforeGuide,

        @NotNull(message = "状态不能为空")
        Integer status,

        String remark,

        @Valid
        @NotEmpty(message = "选项不能为空")
        @Size(min = 2, max = 4, message = "选项数量为2至4个")
        List<QuestionOptionRequest> options
) {
}
