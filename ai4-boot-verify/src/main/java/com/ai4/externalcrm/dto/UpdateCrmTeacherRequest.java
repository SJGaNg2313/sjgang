package com.ai4.externalcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * CRM 修改教师请求，密码为空时保留原密码。
 */
public record UpdateCrmTeacherRequest(
        @NotBlank String username,
        String password,
        @NotBlank String nickname,
        @NotNull Integer crmTeacherId,
        String bz,
        String avatar
) {
}
