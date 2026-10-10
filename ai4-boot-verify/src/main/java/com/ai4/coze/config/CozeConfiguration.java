package com.ai4.coze.config;

import com.coze.openapi.client.auth.LoadAuthConfig;
import com.coze.openapi.service.auth.JWTOAuth;
import com.coze.openapi.service.auth.JWTOAuthClient;
import com.coze.openapi.service.service.CozeAPI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 根据外部 OAuth 配置文件创建 Coze API 客户端。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "ai4.coze", name = "enabled", havingValue = "true")
public class CozeConfiguration {

    @Bean(destroyMethod = "shutdownExecutor")
    public CozeAPI cozeApi(CozeProperties properties) {
        Path configPath = resolveConfigPath(properties.oauthConfigPath());

        try {
            JWTOAuthClient oauthClient = JWTOAuthClient.loadFromConfig(
                    new LoadAuthConfig(configPath.toString())
            );
            return new CozeAPI.Builder()
                    .baseURL(properties.baseUrl())
                    .auth(new JWTOAuth(oauthClient))
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Coze OAuth JWT 客户端初始化失败，请检查 OAuth 配置文件",
                    exception
            );
        }
    }

    private Path resolveConfigPath(String configuredPath) {
        if (!StringUtils.hasText(configuredPath)) {
            throw new IllegalStateException(
                    "启用 Coze 后必须配置 COZE_OAUTH_CONFIG_PATH"
            );
        }

        Path configPath = Path.of(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(configPath)) {
            throw new IllegalStateException(
                    "Coze OAuth 配置文件不存在：" + configPath
            );
        }
        return configPath;
    }
}
