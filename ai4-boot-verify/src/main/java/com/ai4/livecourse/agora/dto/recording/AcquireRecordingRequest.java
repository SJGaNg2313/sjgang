package com.ai4.livecourse.agora.dto.recording;

/** 申请声网网页录制资源的请求参数。 */
public record AcquireRecordingRequest(
        String cname,
        String uid,
        ClientRequest clientRequest
) {

    private static final int RESOURCE_EXPIRED_HOUR = 72;
    private static final int WEB_RECORDING_SCENE = 1;

    public static AcquireRecordingRequest forWebRecording(StartWebRecordingRequest startRequest) {
        return new AcquireRecordingRequest(
                startRequest.cname(),
                startRequest.uid(),
                new ClientRequest(
                        RESOURCE_EXPIRED_HOUR,
                        WEB_RECORDING_SCENE,
                        startRequest.clientRequest()
                )
        );
    }

    public record ClientRequest(
            int resourceExpiredHour,
            int scene,
            StartWebRecordingRequest.ClientRequest startParameter
    ) {
    }
}
