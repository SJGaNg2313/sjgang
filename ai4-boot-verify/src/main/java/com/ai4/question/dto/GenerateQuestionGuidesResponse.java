package com.ai4.question.dto;

import java.util.List;

/** 题前引导和全部选项引导的组合生成结果。 */
public record GenerateQuestionGuidesResponse(
        String beforeGuide,
        List<GeneratedOptionGuideResponse> optionGuides
) {
}
