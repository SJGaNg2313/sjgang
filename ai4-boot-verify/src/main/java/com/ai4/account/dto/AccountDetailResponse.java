package com.ai4.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 学员详情及其最近一节有效直播课。
 */
public record AccountDetailResponse(
        String userId,
        String phone,
        String className,
        String courseId,
        String projectName,
        String teacherName,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime courseTime,

        Long actualDurationSeconds,
        Integer status,
        Boolean attended
) {
}
