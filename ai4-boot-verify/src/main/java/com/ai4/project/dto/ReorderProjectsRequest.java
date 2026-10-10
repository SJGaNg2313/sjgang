package com.ai4.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 项目顺序调整请求，项目 ID 的顺序就是新的项目顺序。 */
public record ReorderProjectsRequest(
        @NotNull(message = "项目顺序不能为空")
        @Size(min = 1, message = "项目顺序不能为空")
        List<@NotNull(message = "项目 ID 不能为空") Long> projectIds
) {
}
