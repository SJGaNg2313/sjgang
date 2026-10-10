package com.ai4.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改项目请求 */
public record UpdateProjectRequest(
        @NotBlank(message = "项目名称不能为空")
        @Size(max = 10, message = "项目名称不能超过 10 个字符")
        String projectName,

        @NotBlank(message = "项目介绍不能为空")
        @Size(max = 25, message = "项目介绍不能超过 25 个字符")
        String projectIntro,

        @NotBlank(message = "项目编码不能为空")
        @Size(max = 64, message = "项目编码不能超过 64 个字符")
        String projectCode
) {
}
