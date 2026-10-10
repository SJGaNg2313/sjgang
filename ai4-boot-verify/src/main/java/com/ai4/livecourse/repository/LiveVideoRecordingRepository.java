package com.ai4.livecourse.repository;

import com.ai4.livecourse.model.LiveCourseRecordingData;
import com.ai4.livecourse.model.LiveCourseVideoData;
import com.ai4.livecourse.model.LiveVideoRecording;

import java.util.List;
import java.util.Optional;

/** 云端录制数据访问接口。 */
public interface LiveVideoRecordingRepository {

    Optional<LiveCourseRecordingData> findCourse(String courseId);

    Optional<LiveVideoRecording> findActiveByCourseId(String courseId);

    Optional<LiveVideoRecording> findActiveByRoomIdAndSecretKey(
            String roomId,
            String secretKey
    );

    Optional<LiveVideoRecording> findBySid(String sid);

    List<String> findClosedCourseIdsWithActiveRecording(int limit);

    void save(LiveVideoRecording recording);

    void updateRunningState(String sid, int status);

    void markServerReady(String sid);

    void markStopped(String sid, String fileList);

    void markFailed(String sid, String fileList);

    Optional<LiveCourseVideoData> findCourseVideo(String courseId);

    void saveCourseVideo(LiveCourseVideoData video);
}
