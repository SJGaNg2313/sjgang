package com.ai4.externalcrm.client;

import com.ai4.externalcrm.config.CrmCallbackProperties;
import com.ai4.externalcrm.dto.CrmClassEndCallbackRequest;
import com.ai4.externalcrm.dto.CrmClassEndStudentRequest;
import com.ai4.externalcrm.dto.CrmCoursePackageLiveCountRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/** 调用 CRM 提供的直播课同步接口。 */
@Component
public class CrmLiveCourseClient {

    private static final Logger log = LoggerFactory.getLogger(CrmLiveCourseClient.class);

    private final RestClient restClient;
    private final CrmCallbackProperties properties;

    public CrmLiveCourseClient(CrmCallbackProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(properties.connectTimeoutSeconds()));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(properties.readTimeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(hasText(properties.baseUrl()) ? properties.baseUrl().trim() : "http://127.0.0.1")
                .requestFactory(requestFactory)
                .build();
    }

    public void updateCoursePackageLiveCount(long packageId, long liveLessonCount) {
        post(
                properties.coursePackageLiveCountPath(),
                new CrmCoursePackageLiveCountRequest(packageId, liveLessonCount),
                "课包直播课数量同步"
        );
    }

    public void callbackClassEnd(CrmClassEndCallbackRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("liveclassid", request.liveclassid());
        payload.put("classdate", request.classdate());
        payload.put("liveurl", request.liveurl());
        payload.put(
                "list",
                request.list().stream()
                        .map(CrmClassEndStudentRequest::toPayload)
                        .toList()
        );
        post(properties.classEndCallbackPath(), payload, "班级直播课结束回调");
    }

    private void post(String path, Object body, String operation) {
        if (!hasText(path)) {
            throw new IllegalStateException(operation + "地址未配置");
        }
        try {
            restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.warn(
                    "{}失败，httpStatus={}，responseBody={}",
                    operation,
                    exception.getStatusCode().value(),
                    exception.getResponseBodyAsString()
            );
            throw new IllegalStateException(operation + "失败", exception);
        } catch (RestClientException exception) {
            log.warn("{}失败，CRM服务不可用，message={}", operation, exception.getMessage());
            throw new IllegalStateException(operation + "失败", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
