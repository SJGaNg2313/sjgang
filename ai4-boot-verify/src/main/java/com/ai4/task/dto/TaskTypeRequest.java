package com.ai4.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 任务内容大类及其允许格式 */
public record TaskTypeRequest(
        @NotBlank(message = "任务类型大类不能为空")
        String category,

        @NotEmpty(message = "任务类型格式不能为空")
        List<@NotBlank(message = "任务格式不能为空") String> formats
) {
}
