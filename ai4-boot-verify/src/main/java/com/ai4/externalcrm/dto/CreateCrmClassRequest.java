package com.ai4.externalcrm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/** CRM 创建班级并自动排直播课的请求。 */
public record CreateCrmClassRequest(
        @NotNull(message = "CRM班级ID不能为空")
        Integer classId,

        @NotBlank(message = "班级名称不能为空")
        String className,

        @NotBlank(message = "上课开始时间不能为空")
        String startTime,

        @NotBlank(message = "上课结束时间不能为空")
        String endTime,

        @NotBlank(message = "课包ID不能为空")
        String packageId,

        @NotBlank(message = "CRM教师ID不能为空")
        String teacherId,

        @NotNull(message = "开始上课日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startClassTime,

        @NotNull(message = "建课间隔不能为空")
        @Positive(message = "建课间隔必须大于0")
        Integer courseRule,

        @NotNull(message = "录制状态不能为空")
        Integer record,

        @NotNull(message = "班级状态不能为空")
        Integer classStatus,

        @NotNull(message = "拖课时间不能为空")
        @PositiveOrZero(message = "拖课时间不能小于0")
        Integer closeDelay
) {
}
