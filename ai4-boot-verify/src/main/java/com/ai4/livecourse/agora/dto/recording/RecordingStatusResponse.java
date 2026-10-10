package com.ai4.livecourse.agora.dto.recording;

import java.util.List;
import java.util.Map;

/** 查询或停止录制时返回的任务状态。 */
public record RecordingStatusResponse(
        String cname,
        String uid,
        String resourceId,
        String sid,
        ServerResponse serverResponse
) {

    public record ServerResponse(
            Integer status,
            List<ExtensionServiceState> extensionServiceState
    ) {
    }

    public record ExtensionServiceState(
            String serviceName,
            Map<String, Object> payload
    ) {
    }
}
