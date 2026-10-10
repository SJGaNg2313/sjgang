package com.ai4.sandtable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 沙盘预计学习收获请求。 */
public record SandtableGainRequest(
        Long id,

        @Size(max = 255, message = "预计学习收获图标不能超过255个字符")
        String icon,

        @NotBlank(message = "预计学习收获文本不能为空")
        @Size(max = 6, message = "预计学习收获文本不能超过6个字符")
        String gain
) {
}
