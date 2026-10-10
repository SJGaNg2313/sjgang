package com.ai4.livecourse.agora.dto.classroom;

import java.util.Map;

/** 创建声网教室的请求参数。 */
public record CreateAgoraRoomRequest(
        String roomName,
        int roomType,
        RoomProperties roomProperties,
        Map<String, Object> roleConfig
) {

    private static final int MAX_HANDS_UP_ACCEPT = 12;
    private static final int STUDENT_LIMIT = 505;

    public static CreateAgoraRoomRequest create(
            String roomName,
            int roomType,
            long startTime,
            int courseDurationMinutes,
            int closeDelayMinutes
    ) {
        if (courseDurationMinutes <= 0) {
            throw new IllegalArgumentException("课程时长必须大于0");
        }
        if (closeDelayMinutes < 0) {
            throw new IllegalArgumentException("拖堂时间不能小于0");
        }

        Schedule schedule = new Schedule(
                startTime,
                Math.multiplyExact(courseDurationMinutes, 60),
                Math.multiplyExact(closeDelayMinutes, 60)
        );
        HandsUp handsUp = new HandsUp(MAX_HANDS_UP_ACCEPT, "");
        RoomProperties properties = new RoomProperties(schedule, new Processes(handsUp));
        Map<String, Object> roleConfig = Map.of("2", Map.of("limit", STUDENT_LIMIT));
        return new CreateAgoraRoomRequest(roomName, roomType, properties, roleConfig);
    }

    public record RoomProperties(Schedule schedule, Processes processes) {
    }

    public record Schedule(long startTime, int duration, int closeDelay) {
    }

    public record Processes(HandsUp handsUp) {
    }

    public record HandsUp(int maxAccept, String defaultAcceptRole) {
    }
}
