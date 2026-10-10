package com.ai4.task.model;

import java.util.List;

/** 项目任务、合伙人与 Skill 的持久化关联模型。 */
public record ProjectTaskPartnerSkills(
        String partnerId,
        List<String> skillIds
) {
}
