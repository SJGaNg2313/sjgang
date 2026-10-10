package com.ai4.report.dto;
import jakarta.validation.constraints.NotBlank;
/** 创建或修改创业报告配置请求。 */
public record SaveProjectReportRequest(
        @NotBlank(message = "点评标准不能为空") String reviewCriteria) { }
