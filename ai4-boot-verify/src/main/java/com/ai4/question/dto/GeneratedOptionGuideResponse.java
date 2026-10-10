package com.ai4.question.dto;

/** 单个选项生成的选后引导，下标与请求中的选项下标一致。 */
public record GeneratedOptionGuideResponse(
        int optionIndex,
        String optionText,
        String afterGuide
) {
}
