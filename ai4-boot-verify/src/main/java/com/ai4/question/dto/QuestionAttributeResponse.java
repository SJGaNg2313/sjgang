package com.ai4.question.dto;

/** 题目管理中用于选择的沙盘属性。 */
public record QuestionAttributeResponse(
        Long attributeId,
        String attributeName
) {
}
