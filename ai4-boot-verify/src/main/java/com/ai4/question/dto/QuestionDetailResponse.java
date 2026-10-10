package com.ai4.question.dto;

import java.util.List;

/** 项目题目详情响应。 */
public record QuestionDetailResponse(
        Long id,
        Long projectId,
        String imageUrl,
        String imageDesc,
        String questionText,
        Integer month,
        String beforeGuide,
        Integer status,
        String remark,
        List<QuestionOptionResponse> options
) {
}