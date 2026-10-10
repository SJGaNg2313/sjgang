package com.ai4.skill.model;

import java.time.LocalDateTime;

/** Skill 完整数据模型。 */
public record Skill(
        String id,
        String skillName,
        String skillContent,
        String skillIcon,
        String triggerConditions,
        String inputParameters,
        String followStep,
        String outputFormat,
        String constraints,
        Integer delFlag,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
