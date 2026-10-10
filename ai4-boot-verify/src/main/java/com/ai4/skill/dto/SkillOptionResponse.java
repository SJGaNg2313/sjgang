package com.ai4.skill.dto;

/** Skill 轻量选项，用于下拉框或多选控件。 */
public record SkillOptionResponse(
        String skillId,
        String skillName,
        String skillIcon
) {
}
