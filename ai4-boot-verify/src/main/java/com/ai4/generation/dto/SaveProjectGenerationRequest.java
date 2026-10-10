package com.ai4.generation.dto;
import jakarta.validation.constraints.NotBlank;
/** 创建或修改项目生成配置请求。 */
public record SaveProjectGenerationRequest(
        @NotBlank(message = "评分标准不能为空") String scoringCriteria) { }
