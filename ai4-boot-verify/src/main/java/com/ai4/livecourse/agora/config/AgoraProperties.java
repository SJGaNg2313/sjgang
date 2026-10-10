package com.ai4.livecourse.agora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai4.agora")
public record AgoraProperties(
        boolean enabled,
        String appId,
        String appCertificate,
        String restApiKey,
        String restApiSecret,
        String classroomBaseUrl,
        String cloudRecordingBaseUrl,
        String recordPageUrl,
        int recordingStorageRegion,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
}
