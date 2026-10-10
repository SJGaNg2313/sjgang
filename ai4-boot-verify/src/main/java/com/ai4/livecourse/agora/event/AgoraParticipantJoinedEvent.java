package com.ai4.livecourse.agora.event;

/** 教师或学生已真实进入声网课堂。 */
public record AgoraParticipantJoinedEvent(
        String courseId,
        String userId,
        int role,
        long joinTime
) {
}
