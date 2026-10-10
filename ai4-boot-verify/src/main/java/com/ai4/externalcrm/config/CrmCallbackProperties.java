package com.ai4.externalcrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** CRM 出站同步接口配置。 */
@ConfigurationProperties(prefix = "ai4.external.crm.callback")
public record CrmCallbackProperties(
        boolean enabled,
        String baseUrl,
        String coursePackageLiveCountPath,
        String classEndCallbackPath,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        int retryScanDelayMs,
        int retryHistoryDays
) {
}
