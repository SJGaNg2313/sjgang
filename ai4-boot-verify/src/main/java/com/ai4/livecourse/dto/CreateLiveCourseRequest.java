package com.ai4.livecourse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

/** 新增直播课请求。 */
public record CreateLiveCourseRequest(
        @NotBlank(message = "班级ID不能为空")
        String classId,

        @NotNull(message = "项目ID不能为空")
        Long projectId,

        @NotNull(message = "开课日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate courseDate,

        @NotNull(message = "上课时间不能为空")
        @JsonFormat(pattern = "HH:mm")
        LocalTime courseStartTime,

        @NotNull(message = "课节时长不能为空")
        @Positive(message = "课节时长必须大于0")
        Integer courseDuration
) {
}
