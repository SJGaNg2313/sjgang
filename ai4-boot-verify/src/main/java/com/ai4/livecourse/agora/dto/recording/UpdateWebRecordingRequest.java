package com.ai4.livecourse.agora.dto.recording;

/** 暂停或继续声网网页录制的请求参数。 */
public record UpdateWebRecordingRequest(
        String cname,
        String uid,
        ClientRequest clientRequest
) {

    public static UpdateWebRecordingRequest of(
            String roomId,
            String recordingUid,
            boolean onhold
    ) {
        return new UpdateWebRecordingRequest(
                roomId,
                recordingUid,
                new ClientRequest(new WebRecordingConfig(onhold))
        );
    }

    public record ClientRequest(WebRecordingConfig webRecordingConfig) {
    }

    public record WebRecordingConfig(boolean onhold) {
    }
}
