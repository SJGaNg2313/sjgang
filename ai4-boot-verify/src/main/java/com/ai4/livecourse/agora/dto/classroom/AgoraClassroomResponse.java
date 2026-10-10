package com.ai4.livecourse.agora.dto.classroom;

/** 声网教室服务的统一响应结构。 */
public record AgoraClassroomResponse<T>(
        Integer code,
        String msg,
        String message,
        T data
) {

    public boolean successful() {
        return code != null && code == 0;
    }

    public String errorMessage() {
        return message != null && !message.isBlank() ? message : msg;
    }
}
