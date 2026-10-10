package com.ai4.livecourse.agora.dto.classroom;

import java.util.List;

/** 声网教室事件分页结果，事件 data 会随 cmd 类型变化。 */
public record AgoraRoomEventPage(
        int total,
        int count,
        String nextId,
        List<AgoraRoomEvent> list
) {
}
