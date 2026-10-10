package com.ai4.livecourse.agora.client;

import com.ai4.livecourse.agora.config.AgoraProperties;
import com.ai4.livecourse.agora.dto.recording.AcquireRecordingRequest;
import com.ai4.livecourse.agora.dto.recording.AcquireRecordingResponse;
import com.ai4.livecourse.agora.dto.recording.RecordingStatusResponse;
import com.ai4.livecourse.agora.dto.recording.StartRecordingResponse;
import com.ai4.livecourse.agora.dto.recording.StartWebRecordingRequest;
import com.ai4.livecourse.agora.dto.recording.StopRecordingRequest;
import com.ai4.livecourse.agora.dto.recording.UpdateWebRecordingRequest;
import com.ai4.livecourse.agora.exception.AgoraClientException;
import com.ai4.livecourse.agora.service.AgoraTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** 调用声网云端网页录制 REST 服务。 */
@Component
public class AgoraCloudRecordingClient extends AgoraRestClientSupport {

    private static final Logger log = LoggerFactory.getLogger(AgoraCloudRecordingClient.class);
    private static final String RECORDING_MODE = "web";

    private final AgoraProperties properties;
    private final AgoraTokenService tokenService;

    public AgoraCloudRecordingClient(
            AgoraProperties properties,
            AgoraTokenService tokenService
    ) {
        super(properties.cloudRecordingBaseUrl(), properties);
        this.properties = properties;
        this.tokenService = tokenService;
    }

    /** 申请云端录制资源。 */
    public AcquireRecordingResponse acquire(AcquireRecordingRequest request) {
        String roomId = requireText(request.cname(), "录制房间号不能为空");
        AcquireRecordingResponse response = execute(
                log,
                "acquireRecording",
                roomId,
                () -> restClient.post()
                        .uri(
                                "/v1/apps/{appId}/cloud_recording/acquire",
                                properties.appId()
                        )
                        .header(HttpHeaders.AUTHORIZATION, tokenService.createRestAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(AcquireRecordingResponse.class)
        );
        requireText(response.resourceId(), "声网申请录制资源未返回resourceId");
        log.info("Agora recording resource acquired, roomId={}", roomId);
        return response;
    }

    /** 启动网页录制。 */
    public StartRecordingResponse startWebRecording(
            String resourceId,
            StartWebRecordingRequest request
    ) {
        String normalizedResourceId = requireText(resourceId, "录制resourceId不能为空");
        String roomId = requireText(request.cname(), "录制房间号不能为空");
        StartRecordingResponse response = execute(
                log,
                "startWebRecording",
                normalizedResourceId,
                () -> restClient.post()
                        .uri(
                                "/v1/apps/{appId}/cloud_recording/resourceid/{resourceId}/mode/{mode}/start",
                                properties.appId(),
                                normalizedResourceId,
                                RECORDING_MODE
                        )
                        .header(HttpHeaders.AUTHORIZATION, tokenService.createRestAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(StartRecordingResponse.class)
        );
        requireText(response.sid(), "声网启动录制未返回sid");
        log.info(
                "Agora web recording started, roomId={}, resourceId={}, sid={}",
                roomId,
                normalizedResourceId,
                response.sid()
        );
        return response;
    }

    /** 查询网页录制任务状态。 */
    public RecordingStatusResponse queryRecording(String resourceId, String sid) {
        String normalizedResourceId = requireText(resourceId, "录制resourceId不能为空");
        String normalizedSid = requireText(sid, "录制sid不能为空");
        return execute(
                log,
                "queryRecording",
                normalizedSid,
                () -> restClient.get()
                        .uri(
                                "/v1/apps/{appId}/cloud_recording/resourceid/{resourceId}/sid/{sid}/mode/{mode}/query",
                                properties.appId(),
                                normalizedResourceId,
                                normalizedSid,
                                RECORDING_MODE
                        )
                        .header(HttpHeaders.AUTHORIZATION, tokenService.createRestAuthorizationHeader())
                        .retrieve()
                        .body(RecordingStatusResponse.class)
        );
    }

    /** 暂停或继续网页录制。 */
    public RecordingStatusResponse updateWebRecording(
            String resourceId,
            String sid,
            UpdateWebRecordingRequest request
    ) {
        String normalizedResourceId = requireText(resourceId, "录制resourceId不能为空");
        String normalizedSid = requireText(sid, "录制sid不能为空");
        return execute(
                log,
                "updateWebRecording",
                normalizedSid,
                () -> restClient.post()
                        .uri(
                                "/v1/apps/{appId}/cloud_recording/resourceid/{resourceId}/sid/{sid}/mode/{mode}/update",
                                properties.appId(),
                                normalizedResourceId,
                                normalizedSid,
                                RECORDING_MODE
                        )
                        .header(HttpHeaders.AUTHORIZATION, tokenService.createRestAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(RecordingStatusResponse.class)
        );
    }

    /** 同步停止网页录制并返回录像文件状态。 */
    public RecordingStatusResponse stopRecording(
            String resourceId,
            String sid,
            StopRecordingRequest request
    ) {
        String normalizedResourceId = requireText(resourceId, "录制resourceId不能为空");
        String normalizedSid = requireText(sid, "录制sid不能为空");
        RecordingStatusResponse response = execute(
                log,
                "stopRecording",
                normalizedSid,
                () -> restClient.post()
                        .uri(
                                "/v1/apps/{appId}/cloud_recording/resourceid/{resourceId}/sid/{sid}/mode/{mode}/stop",
                                properties.appId(),
                                normalizedResourceId,
                                normalizedSid,
                                RECORDING_MODE
                        )
                        .header(HttpHeaders.AUTHORIZATION, tokenService.createRestAuthorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(RecordingStatusResponse.class)
        );
        log.info(
                "Agora web recording stopped, resourceId={}, sid={}",
                normalizedResourceId,
                normalizedSid
        );
        return response;
    }
}
