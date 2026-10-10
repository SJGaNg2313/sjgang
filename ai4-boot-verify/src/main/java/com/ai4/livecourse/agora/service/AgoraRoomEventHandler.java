package com.ai4.livecourse.agora.service;

import com.ai4.common.id.SnowflakeIdGenerator;
import com.ai4.livecourse.agora.dto.classroom.AgoraRoomEvent;
import com.ai4.livecourse.agora.dto.classroom.AgoraRoomStateEventData;
import com.ai4.livecourse.agora.dto.classroom.AgoraRoomUser;
import com.ai4.livecourse.agora.dto.classroom.AgoraRoomUserEventData;
import com.ai4.livecourse.agora.event.AgoraParticipantJoinedEvent;
import com.ai4.livecourse.agora.event.AgoraRoomClosedEvent;
import com.ai4.livecourse.model.AgoraEventCourseData;
import com.ai4.livecourse.repository.LiveCourseEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** 将声网课堂状态和用户进出事件同步到本地数据库。 */
@Service
public class AgoraRoomEventHandler {

    public static final int ROOM_STATE_COMMAND = 1;
    public static final int USER_PRESENCE_COMMAND = 20;

    private static final int TEACHER_ROLE = 1;
    private static final int STUDENT_ROLE = 2;

    private static final Logger log = LoggerFactory.getLogger(AgoraRoomEventHandler.class);

    private final LiveCourseEventRepository eventRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public AgoraRoomEventHandler(
            LiveCourseEventRepository eventRepository,
            SnowflakeIdGenerator idGenerator,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventRepository = eventRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    /** 每条事件在独立事务中完成，成功后才能写入 Redis 幂等标记。 */
    @Transactional
    public void handle(AgoraEventCourseData course, AgoraRoomEvent event) {
        if (event.cmd() == null) {
            log.warn("忽略缺少cmd的声网课堂事件，courseId={}，sequence={}",
                    course.courseId(), event.sequence());
            return;
        }
        if (event.cmd() == USER_PRESENCE_COMMAND) {
            handleUserPresence(course, event);
            return;
        }
        if (event.cmd() == ROOM_STATE_COMMAND) {
            handleRoomState(course, event);
        }
    }

    private void handleUserPresence(
            AgoraEventCourseData course,
            AgoraRoomEvent event
    ) {
        AgoraRoomUserEventData data = readData(event, AgoraRoomUserEventData.class);
        if (data == null) {
            return;
        }

        for (AgoraRoomUser user : data.onlineUsers()) {
            handleUserJoined(course, user);
        }
        for (AgoraRoomUser user : data.offlineUsers()) {
            handleUserLeft(course, user);
        }
    }

    private void handleUserJoined(AgoraEventCourseData course, AgoraRoomUser user) {
        Participant participant = resolveParticipant(course, user);
        if (participant == null) {
            return;
        }

        eventRepository.upsertOnlineUser(
                course.courseId(),
                participant.userId(),
                participant.role(),
                user.updateTime()
        );
        if (participant.role() == STUDENT_ROLE) {
            eventRepository.recordStudentJoined(
                    idGenerator.nextId(),
                    course.courseId(),
                    participant.userId(),
                    user.updateTime(),
                    user.streamUuid(),
                    course.courseTime()
            );
        } else {
            eventRepository.recordTeacherJoined(
                    idGenerator.nextId(),
                    course.courseId(),
                    participant.userId(),
                    user.updateTime(),
                    user.streamUuid(),
                    course.courseTime()
            );
        }
        eventPublisher.publishEvent(new AgoraParticipantJoinedEvent(
                course.courseId(),
                participant.userId(),
                participant.role(),
                user.updateTime()
        ));
    }

    private void handleUserLeft(AgoraEventCourseData course, AgoraRoomUser user) {
        Participant participant = resolveParticipant(course, user);
        if (participant == null) {
            return;
        }

        if (participant.role() == STUDENT_ROLE) {
            eventRepository.recordStudentLeft(
                    idGenerator.nextId(),
                    course.courseId(),
                    participant.userId(),
                    user.updateTime(),
                    user.streamUuid(),
                    course.scheduledEndTime()
            );
        } else {
            eventRepository.recordTeacherLeft(
                    idGenerator.nextId(),
                    course.courseId(),
                    participant.userId(),
                    user.updateTime(),
                    user.streamUuid(),
                    course.scheduledEndTime()
            );
        }
        eventRepository.deleteOnlineUser(course.courseId(), participant.userId());
    }

    private void handleRoomState(AgoraEventCourseData course, AgoraRoomEvent event) {
        AgoraRoomStateEventData data = readData(event, AgoraRoomStateEventData.class);
        if (data == null || data.state() == null) {
            return;
        }

        int state = data.state();
        Long eventTime = switch (state) {
            case 1 -> firstNonNull(data.startTime(), event.ts());
            case 2 -> firstNonNull(data.endTime(), event.ts());
            case 3 -> firstNonNull(data.closeTime(), event.ts());
            default -> event.ts();
        };

        if (state == 3) {
            long closeTime = eventTime == null ? System.currentTimeMillis() : eventTime;
            for (String studentId : eventRepository.findActiveStudentIds(course.courseId())) {
                eventRepository.ensureStudentAttendanceRecord(
                        idGenerator.nextId(),
                        course.courseId(),
                        studentId
                );
            }
            if (StringUtils.hasText(course.teacherId())) {
                eventRepository.ensureTeacherAttendanceRecord(
                        idGenerator.nextId(),
                        course.courseId(),
                        course.teacherId()
                );
            }
            eventRepository.finalizeAttendance(
                    course.courseId(),
                    course.scheduledEndTime(),
                    closeTime
            );
        }

        eventRepository.updateCourseState(course.courseId(), state, eventTime);
        if (state == 3) {
            eventPublisher.publishEvent(new AgoraRoomClosedEvent(course.courseId()));
        }
        log.info(
                "声网课堂状态已同步，courseId={}，roomId={}，state={}",
                course.courseId(),
                course.roomId(),
                state
        );
    }

    private Participant resolveParticipant(
            AgoraEventCourseData course,
            AgoraRoomUser user
    ) {
        if (user == null
                || user.role() == null
                || user.updateTime() == null
                || !StringUtils.hasText(user.userUuid())) {
            log.warn("忽略字段不完整的声网用户事件，courseId={}", course.courseId());
            return null;
        }

        int role = user.role();
        if (role != TEACHER_ROLE && role != STUDENT_ROLE) {
            return null;
        }

        String userId = normalizeUserId(user.userUuid(), role);
        boolean valid = role == TEACHER_ROLE
                ? eventRepository.isAssignedTeacher(course.courseId(), userId)
                : eventRepository.isActiveStudent(course.courseId(), userId);
        if (!valid) {
            log.warn(
                    "忽略不属于当前直播课的声网用户，courseId={}，role={}，userUuid={}",
                    course.courseId(),
                    role,
                    user.userUuid()
            );
            return null;
        }
        return new Participant(userId, role);
    }

    private String normalizeUserId(String userUuid, int role) {
        String normalized = userUuid.trim();
        String rolePrefix = role + "_";
        if (normalized.startsWith(rolePrefix) && normalized.length() > rolePrefix.length()) {
            return normalized.substring(rolePrefix.length());
        }
        return normalized;
    }

    private <T> T readData(AgoraRoomEvent event, Class<T> type) {
        if (event.data() == null || event.data().isNull()) {
            log.warn("忽略缺少data的声网课堂事件，cmd={}，sequence={}",
                    event.cmd(), event.sequence());
            return null;
        }
        try {
            return objectMapper.treeToValue(event.data(), type);
        } catch (JacksonException exception) {
            log.warn(
                    "无法解析声网课堂事件，cmd={}，sequence={}，error={}",
                    event.cmd(),
                    event.sequence(),
                    exception.getMessage()
            );
            return null;
        }
    }

    private Long firstNonNull(Long primary, Long fallback) {
        return primary != null ? primary : fallback;
    }

    private record Participant(String userId, int role) {
    }
}
