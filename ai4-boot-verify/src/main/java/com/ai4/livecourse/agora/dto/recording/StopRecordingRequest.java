package com.ai4.livecourse.agora.dto.recording;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 停止录制的请求参数。 */
public record StopRecordingRequest(
        String cname,
        String uid,
        ClientRequest clientRequest
) {

    public static StopRecordingRequest synchronous(String roomId, String recordingUid) {
        return new StopRecordingRequest(roomId, recordingUid, new ClientRequest(false));
    }

    public record ClientRequest(@JsonProperty("async_stop") boolean asyncStop) {
    }
}
