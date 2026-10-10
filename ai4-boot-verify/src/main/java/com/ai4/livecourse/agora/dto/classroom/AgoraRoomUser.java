package com.ai4.livecourse.agora.dto.classroom;

/** 声网进出课堂事件中的用户。 */
public record AgoraRoomUser(
        String userName,
        String userUuid,
        Integer role,
        String streamUuid,
        Integer type,
        Long updateTime
) {
}
