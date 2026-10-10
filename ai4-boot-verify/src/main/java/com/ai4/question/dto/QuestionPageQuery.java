package com.ai4.question.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 题目分页查询参数，列表固定每页10条。 */
public record QuestionPageQuery(
        @NotNull(message = "项目ID不能为空")
        Long projectId,

        String questionText,

        String remark,

        @Min(value = 1, message = "页码不能小于1")
        Integer pageNum
) {

    public static final int PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE_NUM = 1;

    public int normalizedPageNum() {
        return pageNum == null ? DEFAULT_PAGE_NUM : pageNum;
    }

    public int offset() {
        return (normalizedPageNum() - 1) * PAGE_SIZE;
    }
}
