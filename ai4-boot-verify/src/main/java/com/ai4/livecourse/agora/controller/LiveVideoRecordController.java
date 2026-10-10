package com.ai4.livecourse.agora.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.livecourse.agora.dto.recording.VideoRecordUpdateRequest;
import com.ai4.livecourse.agora.service.LiveVideoRecordingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 接收录制网页和声网云端录制服务的内部回调。 */
@RestController
@RequestMapping("/liveVideoRecord")
public class LiveVideoRecordController {

    private final LiveVideoRecordingService recordingService;

    public LiveVideoRecordController(LiveVideoRecordingService recordingService) {
        this.recordingService = recordingService;
    }

    /** 录制网页加载完成后暂停或继续录制。 */
    @PostMapping("/update")
    public ApiResponse<Void> update(@Valid @RequestBody VideoRecordUpdateRequest request) {
        recordingService.updateRecording(
                request.roomId(),
                request.secretKey(),
                request.pause()
        );
        return ApiResponse.success(null);
    }

    /** 接收录制网页心跳。 */
    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@Valid @RequestBody VideoRecordUpdateRequest request) {
        recordingService.heartbeat(request.roomId(), request.secretKey());
        return ApiResponse.success(null);
    }

    /** 接收声网网页录制状态回调。 */
    @PostMapping("/recordingCallback71")
    public ApiResponse<Void> recordingCallback(@RequestBody Map<String, Object> callback) {
        recordingService.handleRecordingCallback(callback);
        return ApiResponse.success(null);
    }
}
