package com.ai4.ai.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai4.ark")
public record ArkProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotBlank String model,
        @Positive int timeoutSeconds
) {
}
