package com.ai4.project.dto;

/** 项目列表轻量响应 */
public record ProjectListItemResponse(
        Long id,
        String projectName,
        Integer sort,
        String configurationStatus
) {
}
