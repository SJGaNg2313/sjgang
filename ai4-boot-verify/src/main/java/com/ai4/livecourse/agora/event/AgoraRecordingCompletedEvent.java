package com.ai4.livecourse.agora.event;

/** 直播课云端录制已经完成并写入回放地址。 */
public record AgoraRecordingCompletedEvent(String courseId) {
}
