package com.ai4.livecourse.agora.service;

import com.ai4.livecourse.agora.event.AgoraParticipantJoinedEvent;
import com.ai4.livecourse.agora.event.AgoraRoomClosedEvent;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 在课堂事件落库成功后启动或停止云端录制。 */
@Component
public class AgoraRecordingEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgoraRecordingEventListener.class);

    private final LiveVideoRecordingService recordingService;

    public AgoraRecordingEventListener(LiveVideoRecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleParticipantJoined(AgoraParticipantJoinedEvent event) {
        try {
            recordingService.startIfNeeded(
                    event.courseId(),
                    event.joinTime()
            );
        } catch (RuntimeException exception) {
            log.error("课堂进房后启动云端录制失败，courseId={}", event.courseId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Order(0)
    public void handleRoomClosed(AgoraRoomClosedEvent event) {
        try {
            recordingService.stopIfRunning(event.courseId());
        } catch (RuntimeException exception) {
            log.error("课堂关闭后停止云端录制失败，courseId={}", event.courseId(), exception);
        }
    }
}
