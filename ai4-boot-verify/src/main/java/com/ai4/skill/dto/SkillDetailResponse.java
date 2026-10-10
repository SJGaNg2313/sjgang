package com.ai4.skill.dto;

/** Skill 详情，用于查看和回填修改表单。 */
public record SkillDetailResponse(
        String skillId,
        String skillName,
        String skillContent,
        String skillIcon,
        String triggerConditions,
        String inputParameters,
        String followStep,
        String outputFormat,
        String constraints
) {
}
