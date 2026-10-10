package com.ai4.coze.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Coze具体业务工作流配置。 */
@ConfigurationProperties(prefix = "ai4.coze.workflows")
public record CozeWorkflowProperties(
        String questionBeforeGuideId,
        String questionAfterGuideId
) {
}
