package com.ai4.livecourse.agora.client;

import com.ai4.livecourse.agora.config.AgoraProperties;
import com.ai4.livecourse.agora.exception.AgoraClientException;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.function.Supplier;

abstract class AgoraRestClientSupport {

    protected final RestClient restClient;

    protected AgoraRestClientSupport(String baseUrl, AgoraProperties properties) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("声网服务地址未配置");
        }
        if (properties.connectTimeoutSeconds() <= 0 || properties.readTimeoutSeconds() <= 0) {
            throw new IllegalStateException("声网HTTP超时时间必须大于0");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    protected <T> T execute(
            Logger log,
            String operation,
            String target,
            Supplier<T> request
    ) {
        try {
            T result = request.get();
            if (result == null) {
                throw new AgoraClientException(operation, "声网返回空响应：" + operation);
            }
            return result;
        } catch (AgoraClientException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            String requestId = resolveRequestId(exception.getResponseHeaders());
            log.warn(
                    "Agora request failed, operation={}, target={}, status={}, requestId={}",
                    operation,
                    target,
                    exception.getStatusCode().value(),
                    requestId
            );
            throw new AgoraClientException(
                    operation,
                    exception.getStatusCode().value(),
                    requestId,
                    exception
            );
        } catch (RestClientException exception) {
            log.warn(
                    "Agora request failed, operation={}, target={}, transportError={}",
                    operation,
                    target,
                    exception.getClass().getSimpleName()
            );
            throw new AgoraClientException(operation, null, null, exception);
        }
    }

    protected String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String resolveRequestId(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String requestId = headers.getFirst("x-request-id");
        if (!StringUtils.hasText(requestId)) {
            requestId = headers.getFirst("x-agora-request-id");
        }
        return requestId;
    }
}
