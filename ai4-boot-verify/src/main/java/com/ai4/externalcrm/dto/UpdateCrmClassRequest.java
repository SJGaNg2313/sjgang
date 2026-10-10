package com.ai4.externalcrm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * CRM 修改班级并重建未开始直播课的请求。
 */
public record UpdateCrmClassRequest(
        @NotNull Integer classId,
        @NotBlank String className,
        @NotBlank String startTime,
        @NotBlank String endTime,
        @NotBlank String packageId,
        @NotBlank String teacherId,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startClassTime,
        @NotNull @Positive Integer courseRule,
        @NotNull Integer record,
        @NotNull Integer classStatus,
        @NotNull @PositiveOrZero Integer closeDelay
) {
}
