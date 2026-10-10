package com.ai4.livecourse.agora.service;

import com.ai4.livecourse.agora.config.AgoraProperties;
import io.agora.education.EducationTokenBuilder2;
import io.agora.media.RtcTokenBuilder2;
import io.agora.rtm.RtmTokenBuilder2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 统一生成声网 RTC、RTM Token 及 REST API 认证请求头。
 */
@Service
public class AgoraTokenService {

    public static final String SYSTEM_USER_ACCOUNT = "-2";

    private static final int SYSTEM_RTM_TTL_SECONDS = 24 * 60 * 60;
    private static final int OBSERVER_RTM_TTL_SECONDS = 24 * 60 * 60;
    private static final int RECORDER_RTC_TTL_SECONDS = 72 * 60 * 60;
    private static final int EDUCATION_APP_TOKEN_TTL_SECONDS = 24 * 60 * 60;

    private final AgoraProperties properties;

    public AgoraTokenService(AgoraProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成后台调用声网教室服务时使用的系统 RTM Token。
     */
    public String createSystemRtmToken() {
        return createRtmToken(SYSTEM_USER_ACCOUNT, SYSTEM_RTM_TTL_SECONDS);
    }

    /**
     * 生成录制观察者使用的 RTM Token，有效期为24小时。
     */
    public String createObserverRtmToken(String observerUserAccount) {
        return createRtmToken(
                requireText(observerUserAccount, "观察者用户ID不能为空"),
                OBSERVER_RTM_TTL_SECONDS
        );
    }

    /**
     * 生成网页录制使用的 RTC Token，录制用户沿用旧项目的发布者角色。
     */
    public String createRecorderRtcToken(
            String roomId,
            int recordingUid
    ) {
        validateTokenConfiguration();
        if (recordingUid <= 0) {
            throw new IllegalArgumentException("录制用户ID必须大于0");
        }

        String token = new RtcTokenBuilder2().buildTokenWithUid(
                properties.appId(),
                properties.appCertificate(),
                requireText(roomId, "房间号不能为空"),
                recordingUid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                RECORDER_RTC_TTL_SECONDS,
                RECORDER_RTC_TTL_SECONDS
        );
        return requireGeneratedToken(token);
    }

    /**
     * 生成调用灵动课堂 REST API 使用的应用级 EducationToken。
     */
    public String createEducationAuthorizationHeader() {
        validateTokenConfiguration();
        String token = new EducationTokenBuilder2().buildAppToken(
                properties.appId(),
                properties.appCertificate(),
                EDUCATION_APP_TOKEN_TTL_SECONDS
        );
        return "agora token=" + requireGeneratedToken(token);
    }

    /**
     * 生成调用声网 REST API 时使用的 Basic Authorization 请求头。
     */
    public String createRestAuthorizationHeader() {
        validateEnabled();
        String key = requireText(properties.restApiKey(), "声网REST API Key未配置");
        String secret = requireText(properties.restApiSecret(), "声网REST API Secret未配置");
        String credentials = key + ":" + secret;
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String createRtmToken(String userAccount, int ttlSeconds) {
        validateTokenConfiguration();
        String token = new RtmTokenBuilder2().buildToken(
                properties.appId(),
                properties.appCertificate(),
                userAccount,
                ttlSeconds
        );
        return requireGeneratedToken(token);
    }

    private void validateTokenConfiguration() {
        validateEnabled();
        requireText(properties.appId(), "声网App ID未配置");
        requireText(properties.appCertificate(), "声网App Certificate未配置");
    }

    private void validateEnabled() {
        if (!properties.enabled()) {
            throw new IllegalStateException("声网服务未启用");
        }
    }

    private String requireGeneratedToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("声网Token生成失败");
        }
        return token;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }
}
