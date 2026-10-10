package com.ai4.livecourse.agora.client;

import com.ai4.livecourse.agora.config.AgoraProperties;
import com.ai4.livecourse.agora.dto.classroom.AgoraClassroomResponse;
import com.ai4.livecourse.agora.dto.classroom.AgoraRoomEventPage;
import com.ai4.livecourse.agora.dto.classroom.CreateAgoraRoomRequest;
import com.ai4.livecourse.agora.dto.classroom.UpdateAgoraRoomPropertiesRequest;
import com.ai4.livecourse.agora.exception.AgoraClientException;
import com.ai4.livecourse.agora.service.AgoraTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/** 调用声网教室 REST 服务。 */
@Component
public class AgoraClassroomClient extends AgoraRestClientSupport {

    private static final Logger log = LoggerFactory.getLogger(AgoraClassroomClient.class);

    private final AgoraProperties properties;
    private final AgoraTokenService tokenService;

    public AgoraClassroomClient(
            AgoraProperties properties,
            AgoraTokenService tokenService
    ) {
        super(properties.classroomBaseUrl(), properties);
        this.properties = properties;
        this.tokenService = tokenService;
    }

    /** 在声网服务中创建教室。 */
    public void createRoom(String roomId, CreateAgoraRoomRequest request) {
        String normalizedRoomId = requireText(roomId, "房间号不能为空");
        String authorization = tokenService.createEducationAuthorizationHeader();
        execute(log, "createRoom", normalizedRoomId, () -> restClient.post()
                .uri(
                        "/cn/edu/apps/{appId}/v2/rooms/{roomId}",
                        properties.appId(),
                        normalizedRoomId
                )
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity());
        log.info("Agora room created, roomId={}", normalizedRoomId);
    }

    /** 更新声网教室中的自定义属性。 */
    public void updateRoomProperties(String roomId, Map<String, Object> roomProperties) {
        String normalizedRoomId = requireText(roomId, "房间号不能为空");
        if (roomProperties == null || roomProperties.isEmpty()) {
            throw new IllegalArgumentException("教室属性不能为空");
        }
        String authorization = tokenService.createEducationAuthorizationHeader();
        AgoraClassroomResponse<Void> response = execute(
                log,
                "updateRoomProperties",
                normalizedRoomId,
                () -> restClient.put()
                        .uri(
                                "/cn/edu/apps/{appId}/v2/rooms/{roomId}/properties",
                                properties.appId(),
                                normalizedRoomId
                        )
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new UpdateAgoraRoomPropertiesRequest(roomProperties))
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        })
        );
        requireSuccessfulResponse("updateRoomProperties", response);
        log.info("Agora room properties updated, roomId={}", normalizedRoomId);
    }

    /** 按事件游标拉取声网教室事件。 */
    public AgoraRoomEventPage getRoomEvents(
            String roomId,
            String nextId,
            int command
    ) {
        String normalizedRoomId = requireText(roomId, "房间号不能为空");
        String authorization = tokenService.createEducationAuthorizationHeader();
        AgoraClassroomResponse<AgoraRoomEventPage> response = execute(
                log,
                "getRoomEvents",
                normalizedRoomId,
                () -> restClient.get()
                        .uri(uriBuilder -> {
                            uriBuilder.path(
                                            "/cn/edu/apps/{appId}/v2/rooms/{roomId}/sequences"
                                    )
                                    .queryParam("cmd", command);
                            Optional.ofNullable(nextId)
                                    .filter(StringUtils::hasText)
                                    .ifPresent(value -> uriBuilder.queryParam("nextId", value));
                            return uriBuilder.build(properties.appId(), normalizedRoomId);
                        })
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        })
        );
        requireSuccessfulResponse("getRoomEvents", response);
        if (response.data() == null) {
            throw new AgoraClientException("getRoomEvents", "声网教室事件响应中没有data");
        }
        return response.data();
    }

    private void requireSuccessfulResponse(
            String operation,
            AgoraClassroomResponse<?> response
    ) {
        if (!response.successful()) {
            String detail = StringUtils.hasText(response.errorMessage())
                    ? "：" + response.errorMessage()
                    : "";
            throw new AgoraClientException(operation, "声网教室业务请求失败" + detail);
        }
    }
}
