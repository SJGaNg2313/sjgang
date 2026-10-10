package com.ai4.question.model;

import java.time.LocalDateTime;

/** 项目题目主表模型。 */
public record ProjectQuestion(
        Long id,
        Long projectId,
        String imageUrl,
        String imageDesc,
        String questionText,
        Integer month,
        String beforeGuide,
        Integer status,
        String remark,
        Integer delFlag,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}