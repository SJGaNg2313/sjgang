package com.ai4.livecourse.agora.dto.classroom;

import java.util.Map;

/** 修改声网教室自定义属性的请求。 */
public record UpdateAgoraRoomPropertiesRequest(Map<String, Object> properties) {
}
