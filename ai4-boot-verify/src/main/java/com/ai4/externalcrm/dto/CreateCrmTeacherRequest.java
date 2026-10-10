package com.ai4.externalcrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * CRM 新增教师请求。
 */
public record CreateCrmTeacherRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String nickname,
        @NotNull Integer crmTeacherId,
        String bz,
        String avatar,
        @NotEmpty List<@NotNull Long> roleIds
) {
}
