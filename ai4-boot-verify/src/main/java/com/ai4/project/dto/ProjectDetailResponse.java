package com.ai4.project.dto;

/** 项目详情响应 */
public record ProjectDetailResponse(
        String projectName,
        String projectIntro,
        String projectCode,
        Integer sort
) {
}
