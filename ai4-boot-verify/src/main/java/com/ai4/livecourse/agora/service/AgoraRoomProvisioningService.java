package com.ai4.livecourse.agora.service;

import com.ai4.livecourse.agora.client.AgoraClassroomClient;
import com.ai4.livecourse.agora.config.AgoraProperties;
import com.ai4.livecourse.agora.dto.classroom.CreateAgoraRoomRequest;
import com.ai4.livecourse.agora.exception.AgoraClientException;
import com.ai4.livecourse.model.AgoraRoomProvisionData;
import com.ai4.livecourse.repository.LiveCourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/** 为即将开始的直播课创建声网教室。 */
@Service
public class AgoraRoomProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(AgoraRoomProvisioningService.class);
    private static final long LOOK_AHEAD_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final Duration LOCK_TTL = Duration.ofMinutes(1);
    private static final Duration CREATED_MARKER_TTL = Duration.ofDays(7);
    private static final String LOCK_KEY_PREFIX = "ai4:live-course:agora-room:lock:";
    private static final String CREATED_KEY_PREFIX = "ai4:live-course:agora-room:created:";

    private final AgoraProperties agoraProperties;
    private final AgoraClassroomClient agoraClassroomClient;
    private final LiveCourseRepository liveCourseRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public AgoraRoomProvisioningService(
            AgoraProperties agoraProperties,
            AgoraClassroomClient agoraClassroomClient,
            LiveCourseRepository liveCourseRepository,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.agoraProperties = agoraProperties;
        this.agoraClassroomClient = agoraClassroomClient;
        this.liveCourseRepository = liveCourseRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 扫描未来10分钟内开始的直播课，并保证每个教室只成功创建一次。 */
    @Scheduled(fixedDelayString = "${ai4.agora.room-provision-delay-ms:3000}")
    public void provisionUpcomingRooms() {
        if (!agoraProperties.enabled()) {
            return;
        }

        try {
            long currentTime = System.currentTimeMillis();
            List<AgoraRoomProvisionData> courses = liveCourseRepository.findRoomsNeedingProvisioning(
                    currentTime,
                    currentTime + LOOK_AHEAD_MILLIS
            );
            for (AgoraRoomProvisionData course : courses) {
                try {
                    provisionRoom(course);
                } catch (RuntimeException exception) {
                    log.error(
                            "创建声网教室失败，courseId={}，roomId={}",
                            course.courseId(),
                            course.roomId(),
                            exception
                    );
                }
            }
        } catch (RuntimeException exception) {
            log.error("扫描待创建声网教室的直播课失败", exception);
        }
    }

    /** 应用启动后记录调度开关，便于确认服务器环境变量是否生效。 */
    @EventListener(ApplicationReadyEvent.class)
    public void logProvisioningStatus() {
        if (agoraProperties.enabled()) {
            log.info("声网教室创建任务已启用，提前创建时间为10分钟");
        } else {
            log.warn("声网教室创建任务未启用，请检查环境变量AI4_AGORA_ENABLED");
        }
    }

    private void provisionRoom(AgoraRoomProvisionData course) {
        String createdKey = CREATED_KEY_PREFIX + course.courseId();
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(createdKey))) {
            return;
        }

        String lockKey = LOCK_KEY_PREFIX + course.courseId();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(createdKey))) {
                return;
            }
            log.info(
                    "开始创建声网教室，courseId={}，roomId={}，courseTime={}",
                    course.courseId(),
                    course.roomId(),
                    course.courseTime()
            );
            createRoom(course);
            stringRedisTemplate.opsForValue().set(createdKey, "1", CREATED_MARKER_TTL);
            log.info(
                    "声网教室创建完成，courseId={}，roomId={}",
                    course.courseId(),
                    course.roomId()
            );
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void createRoom(AgoraRoomProvisionData course) {
        CreateAgoraRoomRequest request = CreateAgoraRoomRequest.create(
                course.roomName(),
                course.roomType(),
                course.courseTime(),
                course.courseDuration(),
                course.closeDelay()
        );
        try {
            agoraClassroomClient.createRoom(course.roomId(), request);
        } catch (AgoraClientException exception) {
            if (Integer.valueOf(409).equals(exception.httpStatus())) {
                log.info(
                        "声网教室已存在，按创建成功处理，courseId={}，roomId={}",
                        course.courseId(),
                        course.roomId()
                );
                return;
            }
            throw exception;
        }
    }
}
