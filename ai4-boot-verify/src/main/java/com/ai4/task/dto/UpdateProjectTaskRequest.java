package com.ai4.task.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 完整修改项目领取任务请求 */
public record UpdateProjectTaskRequest(
        @NotEmpty(message = "任务类型不能为空")
        List<@NotNull(message = "任务类型项不能为空") @Valid TaskTypeRequest> taskTypes,

        @NotBlank(message = "公司定位不能为空")
        @Size(max = 20, message = "公司定位不能超过 20 个字符")
        String companyPositioning,

        @Size(max = 1000, message = "公司名片地址不能超过 1000 个字符")
        String companyCard,

        @Size(max = 1000, message = "任务图地址不能超过 1000 个字符")
        String taskImage,

        @NotBlank(message = "任务目标不能为空")
        @Size(max = 26, message = "任务目标不能超过 26 个字符")
        String taskObjective,

        @NotNull(message = "任务关键词不能为空")
        @Size(min = 2, max = 5, message = "任务关键词必须配置 2 至 5 个")
        List<@NotBlank(message = "任务关键词不能为空")
                @Size(min = 2, max = 4, message = "单个任务关键词必须为 2 至 4 个字符") String> taskKeywords,

        @Size(max = 1000, message = "提交内容图标地址不能超过 1000 个字符")
        String submissionIcon,

        @NotBlank(message = "提交内容名称不能为空")
        @Size(max = 8, message = "提交内容名称不能超过 8 个字符")
        String submissionName,

        @NotBlank(message = "提交内容简介不能为空")
        @Size(max = 26, message = "提交内容简介不能超过 26 个字符")
        String submissionDescription,

        @NotBlank(message = "任务名称不能为空")
        @Size(max = 8, message = "任务名称不能超过 8 个字符")
        String taskName,

        @NotNull(message = "合伙人配置不能为空")
        @Size(min = 2, max = 4, message = "必须配置 2 至 4 个合伙人")
        List<@NotNull(message = "合伙人配置不能为空") @Valid ProjectTaskPartnerRequest> partners
) {
}
