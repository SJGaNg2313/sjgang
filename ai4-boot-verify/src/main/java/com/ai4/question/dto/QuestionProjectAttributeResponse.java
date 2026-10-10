package com.ai4.question.dto;

import java.util.List;

/** 题目管理中用于选择项目及项目沙盘属性的响应。 */
public record QuestionProjectAttributeResponse(
        Long projectId,
        String projectName,
        List<QuestionAttributeResponse> attributes
) {
}
