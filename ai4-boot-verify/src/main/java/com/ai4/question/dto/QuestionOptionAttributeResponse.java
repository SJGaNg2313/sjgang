package com.ai4.question.dto;

/** 题目详情中的选项属性影响。 */
public record QuestionOptionAttributeResponse(
        Long attributeId,
        String attributeName,
        String attributeEffect
) {
}