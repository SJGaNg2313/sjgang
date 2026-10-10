package com.ai4.livecourse.agora.service;

import com.ai4.livecourse.agora.client.AgoraClassroomClient;
import com.ai4.livecourse.agora.config.AgoraProperties;
import com.ai4.livecourse.agora.dto.classroom.AgoraRoomEvent;
import com.ai4.livecourse.agora.dto.classroom.AgoraRoomEventPage;
import com.ai4.livecourse.model.AgoraEventCourseData;
import com.ai4.livecourse.repository.LiveCourseEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

/** 定时拉取声网课堂状态和用户进出事件。 */
@Service
public class AgoraRoomEventSyncService {

    private static final Logger log = LoggerFactory.getLogger(AgoraRoomEventSyncService.class);

    private static final Duration EVENT_RETENTION_MARGIN = Duration.ofHours(1);
    private static final Duration ROOM_CREATION_LOOK_AHEAD = Duration.ofMinutes(10);
    private static final Duration SYNC_LOCK_TTL = Duration.ofSeconds(30);
    private static final Duration EVENT_KEY_TTL = Duration.ofDays(7);
    private static final int MAX_PAGES_PER_SCAN = 20;

    private static final String CREATED_KEY_PREFIX = "ai4:live-course:agora-room:created:";
    private static final String SYNC_LOCK_KEY_PREFIX = "ai4:live-course:agora-event:lock:";
    private static final String CURSOR_KEY_PREFIX = "ai4:live-course:agora-event:cursor:";
    private static final String PROCESSED_KEY_PREFIX = "ai4:live-course:agora-event:processed:";

    private final AgoraProperties agoraProperties;
    private final AgoraClassroomClient classroomClient;
    private final AgoraRoomEventHandler eventHandler;
    private final LiveCourseEventRepository eventRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public AgoraRoomEventSyncService(
            AgoraProperties agoraProperties,
            AgoraClassroomClient classroomClient,
            AgoraRoomEventHandler eventHandler,
            LiveCourseEventRepository eventRepository,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.agoraProperties = agoraProperties;
        this.classroomClient = classroomClient;
        this.eventHandler = eventHandler;
        this.eventRepository = eventRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 扫描已创建且尚未关闭的课堂，先同步进出事件，再同步状态事件。 */
    @Scheduled(fixedDelayString = "${ai4.agora.event-sync-delay-ms:5000}")
    public void syncRoomEvents() {
        if (!agoraProperties.enabled()) {
            return;
        }

        try {
            long now = System.currentTimeMillis();
            List<AgoraEventCourseData> courses = eventRepository.findCoursesNeedingEventSync(
                    now - EVENT_RETENTION_MARGIN.toMillis(),
                    now + ROOM_CREATION_LOOK_AHEAD.toMillis()
            );
            for (AgoraEventCourseData course : courses) {
                try {
                    syncCourse(course);
                } catch (RuntimeException exception) {
                    log.error(
                            "同步声网课堂事件失败，courseId={}，roomId={}",
                            course.courseId(),
                            course.roomId(),
                            exception
                    );
                }
            }
        } catch (RuntimeException exception) {
            log.error("扫描待同步声网课堂事件的直播课失败", exception);
        }
    }

    private void syncCourse(AgoraEventCourseData course) {
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                CREATED_KEY_PREFIX + course.courseId()))) {
            return;
        }

        String lockKey = SYNC_LOCK_KEY_PREFIX + course.courseId();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                "1",
                SYNC_LOCK_TTL
        );
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            syncCommand(course, AgoraRoomEventHandler.USER_PRESENCE_COMMAND);
            syncCommand(course, AgoraRoomEventHandler.ROOM_STATE_COMMAND);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void syncCommand(AgoraEventCourseData course, int command) {
        String cursorKey = CURSOR_KEY_PREFIX + course.roomId() + ":" + command;
        String requestCursor = stringRedisTemplate.opsForValue().get(cursorKey);

        for (int pageNumber = 0; pageNumber < MAX_PAGES_PER_SCAN; pageNumber++) {
            AgoraRoomEventPage page = classroomClient.getRoomEvents(
                    course.roomId(),
                    requestCursor,
                    command
            );
            processEvents(course, command, page.list());

            String nextCursor = normalizeCursor(page.nextId());
            if (nextCursor == null) {
                return;
            }
            if (nextCursor.equals(requestCursor)) {
                log.warn(
                        "声网课堂事件游标未前进，停止本轮同步，roomId={}，cmd={}，nextId={}",
                        course.roomId(),
                        command,
                        nextCursor
                );
                return;
            }
            stringRedisTemplate.opsForValue().set(cursorKey, nextCursor, EVENT_KEY_TTL);
            requestCursor = nextCursor;
        }

        log.warn(
                "声网课堂事件单轮分页达到上限，等待下一轮继续，roomId={}，cmd={}",
                course.roomId(),
                command
        );
    }

    private void processEvents(
            AgoraEventCourseData course,
            int command,
            List<AgoraRoomEvent> events
    ) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (AgoraRoomEvent event : events) {
            if (event == null || event.sequence() == null) {
                log.warn("忽略缺少sequence的声网课堂事件，roomId={}，cmd={}",
                        course.roomId(), command);
                continue;
            }

            String processedKey = PROCESSED_KEY_PREFIX
                    + course.roomId() + ":" + command + ":" + event.sequence();
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(processedKey))) {
                continue;
            }

            eventHandler.handle(course, event);
            stringRedisTemplate.opsForValue().set(processedKey, "1", EVENT_KEY_TTL);
        }
    }

    private String normalizeCursor(String cursor) {
        return StringUtils.hasText(cursor) ? cursor.trim() : null;
    }
}
