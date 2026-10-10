package com.ai4.livecourse.agora.dto.classroom;

/** cmd=1 时返回的课堂状态数据。 */
public record AgoraRoomStateEventData(
        Integer state,
        Long startTime,
        Long endTime,
        Long closeTime
) {
}
