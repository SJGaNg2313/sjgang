package com.ai4.externalcrm.security;

import com.ai4.common.exception.ErrorCode;
import com.ai4.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** 校验 CRM 外部接口调用方是否位于 Redis IP 白名单中。 */
@Component
public class CrmIpWhitelistInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CrmIpWhitelistInterceptor.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private final StringRedisTemplate redisTemplate;
    private final CrmWhitelistProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    public CrmIpWhitelistInterceptor(
            StringRedisTemplate redisTemplate,
            CrmWhitelistProperties properties,
            ClientIpResolver clientIpResolver,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        ClientIpResolver.ClientIpInfo ipInfo = clientIpResolver.resolve(request);
        if (!ipInfo.resolved()) {
            log.warn(
                    "CRM客户端IP无法识别，已拒绝请求：requestId={}, method={}, uri={}, clientIp={}, "
                            + "remoteIp={}, trustedProxy={}, xRealIp={}, xForwardedFor={}, "
                            + "ipSource={}, failureReason={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    ipInfo.clientIp(),
                    ipInfo.remoteIp(),
                    ipInfo.trustedProxy(),
                    ipInfo.xRealIp(),
                    ipInfo.xForwardedFor(),
                    ipInfo.resolutionSource(),
                    ipInfo.failureReason()
            );
            writeFailure(
                    response,
                    HttpStatus.FORBIDDEN,
                    ErrorCode.CRM_CLIENT_IP_UNRESOLVED,
                    requestId
            );
            return false;
        }

        Boolean allowed;
        try {
            allowed = redisTemplate.opsForSet().isMember(
                    properties.ipWhitelistKey(),
                    ipInfo.clientIp()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "CRM白名单校验异常，已拒绝请求：requestId={}, method={}, uri={}, clientIp={}, "
                            + "remoteIp={}, trustedProxy={}, xRealIp={}, xForwardedFor={}, "
                            + "ipSource={}, redisKey={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    ipInfo.clientIp(),
                    ipInfo.remoteIp(),
                    ipInfo.trustedProxy(),
                    ipInfo.xRealIp(),
                    ipInfo.xForwardedFor(),
                    ipInfo.resolutionSource(),
                    properties.ipWhitelistKey(),
                    exception
            );
            writeFailure(
                    response,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorCode.CRM_IP_WHITELIST_UNAVAILABLE,
                    requestId
            );
            return false;
        }

        if (!Boolean.TRUE.equals(allowed)) {
            log.warn(
                    "CRM白名单校验未通过：requestId={}, method={}, uri={}, clientIp={}, remoteIp={}, "
                            + "trustedProxy={}, xRealIp={}, xForwardedFor={}, ipSource={}, redisKey={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    ipInfo.clientIp(),
                    ipInfo.remoteIp(),
                    ipInfo.trustedProxy(),
                    ipInfo.xRealIp(),
                    ipInfo.xForwardedFor(),
                    ipInfo.resolutionSource(),
                    properties.ipWhitelistKey()
            );
            writeFailure(
                    response,
                    HttpStatus.FORBIDDEN,
                    ErrorCode.CRM_IP_NOT_ALLOWED,
                    requestId
            );
            return false;
        }

        log.info(
                "CRM白名单校验通过：requestId={}, method={}, uri={}, clientIp={}, remoteIp={}, "
                        + "trustedProxy={}, xRealIp={}, xForwardedFor={}, ipSource={}, redisKey={}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                ipInfo.clientIp(),
                ipInfo.remoteIp(),
                ipInfo.trustedProxy(),
                ipInfo.xRealIp(),
                ipInfo.xForwardedFor(),
                ipInfo.resolutionSource(),
                properties.ipWhitelistKey()
        );
        return true;
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            return UUID.randomUUID().toString();
        }
        String sanitized = requestId.replace('\r', '_').replace('\n', '_').trim();
        return sanitized.length() <= 100 ? sanitized : sanitized.substring(0, 100);
    }

    private void writeFailure(
            HttpServletResponse response,
            HttpStatus status,
            ErrorCode errorCode,
            String requestId
    ) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                new ApiResponse<Void>(
                        errorCode.code(),
                        errorCode.message(),
                        null,
                        requestId
                )
        );
    }
}
