package com.ai4.externalcrm.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * CRM 批量绑定或移除班级学员请求。
 */
public record CrmClassStudentsRequest(
        @NotNull Integer classId,
        @NotEmpty List<String> phoneList
) {
}
