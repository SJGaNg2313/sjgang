package com.ai4.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 项目任务中一个合伙人与其使用 Skill 的配置。 */
public record ProjectTaskPartnerRequest(
        @NotBlank(message = "合伙人 ID 不能为空")
        String partnerId,
        @NotNull(message = "Skill 不能为空")
        @Size(min = 1, max = 6, message = "每个合伙人必须选择 1 至 6 个 Skill")
        List<@NotBlank(message = "Skill ID 不能为空") String> skillIds
) {
}
