package com.ai4.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai4.oss")
public record OssProperties (
        String endpoint,
        String accessKeyId,
        String accessKeySecret,
        String bucketName,
        String publicBaseUrl
){
}
