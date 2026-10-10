package com.ai4.account.client;

import com.ai4.account.client.dto.InternalCreateUserRequest;
import com.ai4.account.client.dto.InternalCreateUserResponse;
import com.ai4.account.client.dto.InternalResetPasswordRequest;
import com.ai4.account.client.dto.InternalUserApiResponse;
import com.ai4.account.config.ClientUserProperties;
import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/**
 * 调用客户端服务提供的内部用户接口。
 */
@Component
public class ClientUserClient {

    private static final Logger log = LoggerFactory.getLogger(ClientUserClient.class);
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final int SUCCESS_CODE = 200;

    private final RestClient restClient;
    private final ClientUserProperties properties;

    public ClientUserClient(ClientUserProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 由客户端服务创建用户并按其规则保存密码。
     */
    public InternalCreateUserResponse createUser(InternalCreateUserRequest request) {
        requireInternalToken();
        try {
            InternalUserApiResponse<InternalCreateUserResponse> response = restClient.post()
                    .uri("/internal/users")
                    .header(INTERNAL_TOKEN_HEADER, properties.internalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return requireData(response);
        } catch (RestClientResponseException exception) {
            throw translateResponseException(exception);
        } catch (RestClientException exception) {
            log.warn("调用客户端用户创建接口失败，status=unavailable");
            throw new BizException(ErrorCode.CLIENT_USER_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 由客户端服务重置用户密码并清理旧登录态。
     */
    public void resetPassword(InternalResetPasswordRequest request) {
        requireInternalToken();
        try {
            InternalUserApiResponse<Void> response = restClient.post()
                    .uri("/internal/users/reset-password")
                    .header(INTERNAL_TOKEN_HEADER, properties.internalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            requireSuccess(response);
        } catch (RestClientResponseException exception) {
            throw translateResponseException(exception);
        } catch (RestClientException exception) {
            log.warn("调用客户端用户密码重置接口失败，status=unavailable");
            throw new BizException(ErrorCode.CLIENT_USER_SERVICE_UNAVAILABLE);
        }
    }

    private InternalCreateUserResponse requireData(
            InternalUserApiResponse<InternalCreateUserResponse> response
    ) {
        requireSuccess(response);
        if (response.data() == null) {
            throw new BizException(ErrorCode.CLIENT_USER_REQUEST_FAILED);
        }
        return response.data();
    }

    private void requireSuccess(InternalUserApiResponse<?> response) {
        if (response == null || response.code() == null) {
            throw new BizException(ErrorCode.CLIENT_USER_REQUEST_FAILED);
        }
        if (response.code() == SUCCESS_CODE) {
            return;
        }

        throw mapBusinessError(response.code(), response.msg());
    }

    private BizException translateResponseException(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        String message = extractResponseMessage(exception);
        BizException businessException = mapBusinessError(status, message);
        if (status == 401 || status == 403) {
            log.warn("客户端用户内部接口认证失败，httpStatus={}", status);
        }
        return businessException;
    }

    private BizException mapBusinessError(int code, String message) {
        return switch (code) {
            case 401, 403 -> new BizException(ErrorCode.CLIENT_USER_AUTHENTICATION_FAILED);
            case 404 -> new BizException(ErrorCode.ACCOUNT_NOT_FOUND);
            case 409 -> new BizException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
            case 503 -> new BizException(ErrorCode.CLIENT_USER_SERVICE_UNAVAILABLE);
            default -> new BizException(
                    ErrorCode.CLIENT_USER_REQUEST_FAILED.code(),
                    hasText(message) ? message : ErrorCode.CLIENT_USER_REQUEST_FAILED.message()
            );
        };
    }

    @SuppressWarnings("rawtypes")
    private String extractResponseMessage(RestClientResponseException exception) {
        try {
            InternalUserApiResponse response = exception.getResponseBodyAs(InternalUserApiResponse.class);
            return response == null ? null : response.msg();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void requireInternalToken() {
        if (!hasText(properties.internalToken())) {
            throw new BizException(ErrorCode.CLIENT_USER_NOT_CONFIGURED);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
