package com.ai4.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新增 Skill 请求。 */
public record CreateSkillRequest(
        @NotBlank(message = "Skill名称不能为空")
        @Size(max = 4, message = "Skill名称不能超过4个字符")
        String skillName,

        @NotBlank(message = "Skill内容不能为空")
        String skillContent,

        @Size(max = 255, message = "Skill图标地址不能超过255个字符")
        String skillIcon,

        @NotBlank(message = "触发条件不能为空")
        String triggerConditions,

        @NotBlank(message = "输入参数不能为空")
        String inputParameters,

        @NotBlank(message = "执行步骤不能为空")
        String followStep,

        @NotBlank(message = "输出格式不能为空")
        String outputFormat,

        @NotBlank(message = "约束不能为空")
        String constraints
) {
}
