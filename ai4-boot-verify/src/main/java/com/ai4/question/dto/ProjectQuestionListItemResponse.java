package com.ai4.question.dto;

/** 项目题目分页列表项。 */
public record ProjectQuestionListItemResponse(
        Long id,
        Integer month,
        String questionText,
        Integer status,
        String remark
) {
}
