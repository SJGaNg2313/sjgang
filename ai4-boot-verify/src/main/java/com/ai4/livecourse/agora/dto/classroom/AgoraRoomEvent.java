package com.ai4.livecourse.agora.dto.classroom;

import tools.jackson.databind.JsonNode;

/** 声网教室事件，data 的结构由 cmd 决定。 */
public record AgoraRoomEvent(
        String roomUuid,
        Integer cmd,
        Long sequence,
        Integer version,
        Long ts,
        JsonNode data
) {
}
