package com.ai4.common.config;

import com.ai4.auth.interceptor.JwtAuthenticationInterceptor;
import com.ai4.auth.interceptor.PermissionAuthorizationInterceptor;
import com.ai4.externalcrm.security.CrmIpWhitelistInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 配置 Spring MVC 的通用行为。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    private final PermissionAuthorizationInterceptor permissionAuthorizationInterceptor;
    private final CrmIpWhitelistInterceptor crmIpWhitelistInterceptor;
    private final CorsProperties corsProperties;

    public WebMvcConfig(
            JwtAuthenticationInterceptor jwtAuthenticationInterceptor,
            PermissionAuthorizationInterceptor permissionAuthorizationInterceptor,
            CrmIpWhitelistInterceptor crmIpWhitelistInterceptor,
            CorsProperties corsProperties
    ) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
        this.permissionAuthorizationInterceptor = permissionAuthorizationInterceptor;
        this.crmIpWhitelistInterceptor = crmIpWhitelistInterceptor;
        this.corsProperties = corsProperties;
    }

    /** 允许录制页面跨域调用更新和心跳接口。 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        configureRecordingCors(registry.addMapping("/liveVideoRecord/update"));
        configureRecordingCors(registry.addMapping("/liveVideoRecord/heartbeat"));
    }

    private void configureRecordingCors(CorsRegistration registration) {
        registration
                .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods(HttpMethod.POST.name(), HttpMethod.OPTIONS.name())
                .allowedHeaders(
                        HttpHeaders.CONTENT_TYPE,
                        HttpHeaders.ACCEPT,
                        HttpHeaders.ORIGIN
                )
                .maxAge(3600);
    }

    /** 注册认证和权限拦截器，并配置公开接口。 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(crmIpWhitelistInterceptor)
                .addPathPatterns("/external/**")
                .order(-1);

        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/captcha",
                        "/external/**",
                        "/liveVideoRecord/**",
                        "/actuator/health",
                        "/error"
                )
                .order(0);

        registry.addInterceptor(permissionAuthorizationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/captcha",
                        "/external/**",
                        "/liveVideoRecord/**",
                        "/actuator/health",
                        "/error"
                )
                .order(1);
    }
}
