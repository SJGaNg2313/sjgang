package com.ai4.question.dto;

import java.util.List;

/** 题目详情中的选项。 */
public record QuestionOptionResponse(
        Long id,
        String optionText,
        String impactType,
        String afterGuide,
        List<QuestionOptionAttributeResponse> attributes
) {
}