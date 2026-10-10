package com.ai4.livecourse.model;

/** 声网云端录制任务。 */
public record LiveVideoRecording(
        String courseId,
        String resourceId,
        String sid,
        int status,
        String fileList,
        long createTime,
        String userId,
        int roleType,
        String uid,
        String url,
        int maxRecordingHour,
        int server,
        String secretKey,
        String roomId
) {
}
