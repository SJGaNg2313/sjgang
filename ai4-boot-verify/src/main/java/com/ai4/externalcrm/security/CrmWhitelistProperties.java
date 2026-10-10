package com.ai4.externalcrm.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** CRM 外部接口 IP 白名单配置。 */
@ConfigurationProperties(prefix = "ai4.external.crm")
public record CrmWhitelistProperties(
        String ipWhitelistKey,
        List<String> trustedProxyIps
) {
}
