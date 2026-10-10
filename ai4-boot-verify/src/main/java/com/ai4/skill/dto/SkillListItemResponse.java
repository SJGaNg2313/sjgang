package com.ai4.skill.dto;

/** Skill 分页列表项。 */
public record SkillListItemResponse(
        String skillId,
        String skillName,
        String skillContent,
        String skillIcon,
        long partnerCount
) {
}
