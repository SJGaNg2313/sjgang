package com.ai4.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改指定下标的公司名称候选。 */
public record UpdateCompanyNameRequest(
        @NotBlank(message = "公司名称不能为空")
        @Size(max = 7, message = "公司名称不能超过 7 个字符")
        String companyName
) {
}
