package com.ai4.livecourse.agora.dto.classroom;

import java.util.List;

/** cmd=20 时返回的用户进出课堂数据。 */
public record AgoraRoomUserEventData(
        Integer total,
        List<AgoraRoomUser> onlineUsers,
        List<AgoraRoomUser> offlineUsers
) {

    public AgoraRoomUserEventData {
        onlineUsers = onlineUsers == null ? List.of() : List.copyOf(onlineUsers);
        offlineUsers = offlineUsers == null ? List.of() : List.copyOf(offlineUsers);
    }
}
