package com.ai4.livecourse.model;

/** 创建声网教室所需的直播课数据。 */
public record AgoraRoomProvisionData(
        String courseId,
        String roomId,
        String roomName,
        int roomType,
        long courseTime,
        int courseDuration,
        int closeDelay
) {
}
