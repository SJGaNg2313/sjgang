package com.ai4.task.dto;

import java.util.List;

/** 项目任务中一个合伙人与其使用 Skill 的配置响应。 */
public record ProjectTaskPartnerResponse(
        String partnerId,
        List<String> skillIds
) {
}
