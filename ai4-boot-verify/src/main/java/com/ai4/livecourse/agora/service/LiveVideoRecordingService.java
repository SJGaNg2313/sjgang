package com.ai4.livecourse.agora.service;

import com.ai4.common.exception.BizException;
import com.ai4.file.config.OssProperties;
import com.ai4.livecourse.agora.client.AgoraClassroomClient;
import com.ai4.livecourse.agora.client.AgoraCloudRecordingClient;
import com.ai4.livecourse.agora.config.AgoraProperties;
import com.ai4.livecourse.agora.dto.recording.AcquireRecordingRequest;
import com.ai4.livecourse.agora.dto.recording.AcquireRecordingResponse;
import com.ai4.livecourse.agora.dto.recording.RecordingStatusResponse;
import com.ai4.livecourse.agora.dto.recording.StartRecordingResponse;
import com.ai4.livecourse.agora.dto.recording.StartWebRecordingRequest;
import com.ai4.livecourse.agora.dto.recording.StopRecordingRequest;
import com.ai4.livecourse.agora.dto.recording.UpdateWebRecordingRequest;
import com.ai4.livecourse.agora.event.AgoraRecordingCompletedEvent;
import com.ai4.livecourse.model.LiveCourseRecordingData;
import com.ai4.livecourse.model.LiveCourseVideoData;
import com.ai4.livecourse.model.LiveVideoRecording;
import com.ai4.livecourse.repository.LiveVideoRecordingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** 按旧项目流程编排声网网页录制的启动、控制和停止。 */
@Service
public class LiveVideoRecordingService {

    private static final Logger log = LoggerFactory.getLogger(LiveVideoRecordingService.class);

    private static final int RECORDING_ENABLED = 1;
    private static final int RECORDING_PREPARING = -2;
    private static final int RECORDING_PAUSED = 0;
    private static final int RECORDING_RUNNING = 1;
    private static final int RECORDING_STOPPED = 2;
    private static final int RECORDING_FAILED = -1;
    private static final int SERVER_STARTING = 0;
    private static final int ROLE_TEACHER = 1;

    private static final String RECORDING_SERVICE_NAME = "web_recorder_service";
    private static final String LOCK_KEY_PREFIX = "ai4:live-course:recording:lock:";
    private static final String UID_KEY_PREFIX = "ai4:live-course:recording:uid:";
    private static final String CALLBACK_KEY_PREFIX = "ai4:live-course:recording:callback:";
    private static final String OBSERVER_USER_KEY_PREFIX = "live:Observer:user:";
    private static final String OBSERVER_COURSE_KEY_PREFIX = "live:Observer:course:0:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);
    private static final Duration UID_TTL = Duration.ofHours(73);
    private static final Duration CALLBACK_TTL = Duration.ofDays(7);
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] "
                            + "then return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class
            );

    private final LiveVideoRecordingRepository recordingRepository;
    private final AgoraCloudRecordingClient recordingClient;
    private final AgoraClassroomClient classroomClient;
    private final AgoraTokenService tokenService;
    private final AgoraProperties agoraProperties;
    private final OssProperties ossProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public LiveVideoRecordingService(
            LiveVideoRecordingRepository recordingRepository,
            AgoraCloudRecordingClient recordingClient,
            AgoraClassroomClient classroomClient,
            AgoraTokenService tokenService,
            AgoraProperties agoraProperties,
            OssProperties ossProperties,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.recordingRepository = recordingRepository;
        this.recordingClient = recordingClient;
        this.classroomClient = classroomClient;
        this.tokenService = tokenService;
        this.agoraProperties = agoraProperties;
        this.ossProperties = ossProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    /** 首位用户在开课后进入时启动录制，重复进房不会重复创建任务。 */
    public void startIfNeeded(String courseId, long joinTime) {
        LiveCourseRecordingData course = recordingRepository.findCourse(courseId).orElse(null);
        if (course == null
                || course.record() != RECORDING_ENABLED
                || course.status() == 3
                || joinTime < course.courseTime()) {
            return;
        }

        withCourseLock(courseId, () -> {
            if (recordingRepository.findActiveByCourseId(courseId).isPresent()) {
                return;
            }
            startRecording(course);
        });
    }

    /** 课堂关闭时停止仍在运行的录制任务。 */
    public void stopIfRunning(String courseId) {
        withCourseLock(courseId, () -> recordingRepository.findActiveByCourseId(courseId)
                .ifPresent(this::stopRecording));
    }

    /** 录制网页加载完成后暂停或继续实际录制。 */
    public void updateRecording(String roomId, String secretKey, Integer pause) {
        LiveVideoRecording recording = recordingRepository
                .findActiveByRoomIdAndSecretKey(roomId, secretKey)
                .orElseThrow(() -> new BizException("RECORDING_NOT_FOUND", "暂无此录制"));
        boolean onhold = Integer.valueOf(1).equals(pause);
        recordingClient.updateWebRecording(
                recording.resourceId(),
                recording.sid(),
                UpdateWebRecordingRequest.of(roomId, recording.uid(), onhold)
        );
        int status = onhold ? RECORDING_PAUSED : RECORDING_RUNNING;
        recordingRepository.updateRunningState(recording.sid(), status);
        updateRoomRecordingStatus(roomId, status);
        if (!onhold) {
            saveDeterministicM3u8(recording);
        }
    }

    /** 保存录制网页心跳，供排查网页录制进程状态使用。 */
    public void heartbeat(String roomId, String secretKey) {
        LiveVideoRecording recording = recordingRepository
                .findActiveByRoomIdAndSecretKey(roomId, secretKey)
                .orElseThrow(() -> new BizException("RECORDING_NOT_FOUND", "暂无此录制"));
        stringRedisTemplate.opsForValue().set(
                "ai4:live-course:recording:heartbeat:" + recording.sid(),
                Long.toString(System.currentTimeMillis()),
                Duration.ofDays(1)
        );
    }

    /** 兼容旧录制流程中的声网录制回调。 */
    public void handleRecordingCallback(Map<String, Object> callback) {
        if (callback == null) {
            return;
        }
        String sid = text(callback.get("sid"));
        Integer eventType = integer(callback.get("eventType"));
        Map<String, Object> payload = map(callback.get("payload"));
        if (!StringUtils.hasText(sid) || eventType == null || payload == null) {
            return;
        }
        if (eventType != 70 && eventType != 71) {
            return;
        }

        LiveVideoRecording recording = recordingRepository.findBySid(sid).orElse(null);
        if (recording == null) {
            return;
        }
        Map<String, Object> details = eventType == 71 ? map(payload.get("details")) : null;
        Integer code = details == null ? null : integer(details.get("code"));
        if (eventType == 71 && code == null) {
            return;
        }

        String callbackKey = CALLBACK_KEY_PREFIX
                + sid + ":" + eventType + ":" + text(payload.get("sequence"));
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(
                callbackKey,
                "1",
                CALLBACK_TTL
        );
        if (!Boolean.TRUE.equals(first)) {
            return;
        }

        try {
            if (eventType == 70) {
                recordingRepository.markServerReady(sid);
                return;
            }

            List<Map<String, Object>> files = fileEntries(details.get("fileList"));
            if (code == 0 || code == 1) {
                completeRecording(recording, files);
            } else {
                recordingRepository.markFailed(recording.sid(), writeJson(files));
                updateRoomRecordingStatus(recording.roomId(), RECORDING_FAILED);
            }
        } catch (RuntimeException exception) {
            stringRedisTemplate.delete(callbackKey);
            throw exception;
        }
    }

    /** 兜底停止应用重启或临时请求失败后遗留的已关闭课堂录制。 */
    @Scheduled(fixedDelayString = "${ai4.agora.recording-recovery-delay-ms:30000}")
    public void stopClosedCourseRecordings() {
        if (!agoraProperties.enabled()) {
            return;
        }
        for (String courseId : recordingRepository.findClosedCourseIdsWithActiveRecording(100)) {
            try {
                stopIfRunning(courseId);
            } catch (RuntimeException exception) {
                log.error("停止已关闭课堂的录制失败，courseId={}", courseId, exception);
            }
        }
    }

    private void startRecording(LiveCourseRecordingData course) {
        String recordingUid = reserveRecordingUid();
        String secretKey = UUID.randomUUID().toString();
        try {
            int uid = Integer.parseInt(recordingUid);
            int maxRecordingHour = calculateMaxRecordingHour(course);
            String fileBaseUrl = recordingFileBaseUrl();
            String recordPageUrl = buildRecordPageUrl(course, recordingUid, secretKey);
            String rtcToken = tokenService.createRecorderRtcToken(course.roomId(), uid);
            StartWebRecordingRequest startRequest = StartWebRecordingRequest.create(
                    course.roomId(),
                    uid,
                    rtcToken,
                    recordPageUrl,
                    maxRecordingHour,
                    StartWebRecordingRequest.StorageConfig.aliyun(
                            agoraProperties.recordingStorageRegion(),
                            requireText(ossProperties.bucketName(), "OSS Bucket未配置"),
                            requireText(ossProperties.accessKeyId(), "OSS AccessKey ID未配置"),
                            requireText(ossProperties.accessKeySecret(), "OSS AccessKey Secret未配置")
                    )
            );
            AcquireRecordingResponse acquired = recordingClient.acquire(
                    AcquireRecordingRequest.forWebRecording(startRequest)
            );
            StartRecordingResponse started = recordingClient.startWebRecording(
                    acquired.resourceId(),
                    startRequest
            );
            LiveVideoRecording recording = new LiveVideoRecording(
                    course.courseId(),
                    acquired.resourceId(),
                    started.sid(),
                    RECORDING_PREPARING,
                    "[]",
                    System.currentTimeMillis(),
                    course.teacherId(),
                    ROLE_TEACHER,
                    recordingUid,
                    fileBaseUrl,
                    maxRecordingHour,
                    SERVER_STARTING,
                    secretKey,
                    course.roomId()
            );
            recordingRepository.save(recording);
            updateRoomRecordingStatus(course.roomId(), RECORDING_PREPARING);
            log.info(
                    "声网云端录制启动请求已完成，courseId={}，roomId={}，sid={}",
                    course.courseId(),
                    course.roomId(),
                    started.sid()
            );
        } catch (RuntimeException exception) {
            stringRedisTemplate.delete(UID_KEY_PREFIX + recordingUid);
            stringRedisTemplate.delete(OBSERVER_USER_KEY_PREFIX + recordingUid);
            stringRedisTemplate.delete(OBSERVER_COURSE_KEY_PREFIX + course.roomId());
            updateRoomRecordingStatus(course.roomId(), RECORDING_FAILED);
            throw exception;
        }
    }

    private void stopRecording(LiveVideoRecording recording) {
        RecordingStatusResponse response = recordingClient.stopRecording(
                recording.resourceId(),
                recording.sid(),
                StopRecordingRequest.synchronous(recording.roomId(), recording.uid())
        );
        completeRecording(recording, extractFiles(response));
        log.info(
                "声网云端录制已停止，courseId={}，roomId={}，sid={}",
                recording.courseId(),
                recording.roomId(),
                recording.sid()
        );
    }

    private void completeRecording(
            LiveVideoRecording recording,
            List<Map<String, Object>> files
    ) {
        recordingRepository.markStopped(recording.sid(), writeJson(files));
        mergeCourseVideo(recording, files);
        updateRoomRecordingStatus(recording.roomId(), RECORDING_STOPPED);
        stringRedisTemplate.delete(UID_KEY_PREFIX + recording.uid());
        eventPublisher.publishEvent(new AgoraRecordingCompletedEvent(recording.courseId()));
    }

    private void mergeCourseVideo(
            LiveVideoRecording recording,
            List<Map<String, Object>> files
    ) {
        Optional<LiveCourseVideoData> existing = recordingRepository
                .findCourseVideo(recording.courseId());
        LinkedHashSet<String> mp4Urls = new LinkedHashSet<>(
                readUrls(existing.map(LiveCourseVideoData::fileList).orElse(null))
        );
        LinkedHashSet<String> m3u8Urls = new LinkedHashSet<>(
                readUrls(existing.map(LiveCourseVideoData::m3u8).orElse(null))
        );
        Long startTime = existing.map(LiveCourseVideoData::startTime).orElse(null);

        for (Map<String, Object> file : files) {
            String filename = text(file.get("filename"));
            if (!StringUtils.hasText(filename)) {
                continue;
            }
            String url = recording.url() + filename;
            if (filename.toLowerCase().endsWith(".mp4")) {
                mp4Urls.add(url);
            } else if (filename.toLowerCase().endsWith(".m3u8")) {
                m3u8Urls.add(url);
            }
            Long sliceStartTime = longValue(file.get("sliceStartTime"));
            if (sliceStartTime != null && (startTime == null || sliceStartTime < startTime)) {
                startTime = sliceStartTime;
            }
        }

        recordingRepository.saveCourseVideo(new LiveCourseVideoData(
                recording.courseId(),
                writeJson(new ArrayList<>(mp4Urls)),
                writeJson(new ArrayList<>(m3u8Urls)),
                startTime
        ));
    }

    private void saveDeterministicM3u8(LiveVideoRecording recording) {
        Optional<LiveCourseVideoData> existing = recordingRepository
                .findCourseVideo(recording.courseId());
        LinkedHashSet<String> m3u8Urls = new LinkedHashSet<>(
                readUrls(existing.map(LiveCourseVideoData::m3u8).orElse(null))
        );
        m3u8Urls.add(recording.url() + recording.sid() + "_" + recording.roomId() + ".m3u8");
        recordingRepository.saveCourseVideo(new LiveCourseVideoData(
                recording.courseId(),
                existing.map(LiveCourseVideoData::fileList).orElse("[]"),
                writeJson(new ArrayList<>(m3u8Urls)),
                existing.map(LiveCourseVideoData::startTime).orElse(null)
        ));
    }

    private String buildRecordPageUrl(
            LiveCourseRecordingData course,
            String recordingUid,
            String secretKey
    ) {
        String template = requireText(
                agoraProperties.recordPageUrl(),
                "声网录制页面URL未配置"
        );
        String rtmToken = tokenService.createObserverRtmToken(recordingUid);
        String gameToken = UUID.randomUUID().toString();
        saveObserverIdentity(course, recordingUid, gameToken);
        String roomName = StringUtils.hasText(course.roomName())
                ? course.roomName()
                : "未设置课程名称";
        String[] values = {
                recordingUid,
                "录制",
                course.roomId(),
                course.courseId(),
                roomName,
                rtmToken,
                Integer.toString(course.courseDuration() * 60),
                agoraProperties.appId(),
                gameToken,
                secretKey,
                Integer.toString(course.roomType()),
                "0"
        };
        String url = template;
        for (int index = 0; index < values.length; index++) {
            url = url.replace(
                    "{" + index + "}",
                    URLEncoder.encode(values[index], StandardCharsets.UTF_8)
            );
        }
        return url;
    }

    private void saveObserverIdentity(
            LiveCourseRecordingData course,
            String recordingUid,
            String gameToken
    ) {
        Map<String, Object> observer = new LinkedHashMap<>();
        observer.put("userId", recordingUid);
        observer.put("token", gameToken);
        observer.put("roleType", 0);
        String observerJson = writeJson(observer);
        Duration ttl = Duration.ofMinutes(
                Math.max(1, course.courseDuration() + course.closeDelay() + 1L)
        );
        stringRedisTemplate.opsForValue().set(
                OBSERVER_USER_KEY_PREFIX + recordingUid,
                observerJson,
                ttl
        );
        stringRedisTemplate.opsForValue().set(
                OBSERVER_COURSE_KEY_PREFIX + course.roomId(),
                observerJson,
                ttl
        );
    }

    private int calculateMaxRecordingHour(LiveCourseRecordingData course) {
        int totalMinutes = course.courseDuration() + course.closeDelay();
        return Math.max(1, (totalMinutes + 59) / 60 + 1);
    }

    private String reserveRecordingUid() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String uid = Integer.toString(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
            Boolean reserved = stringRedisTemplate.opsForValue().setIfAbsent(
                    UID_KEY_PREFIX + uid,
                    "1",
                    UID_TTL
            );
            if (Boolean.TRUE.equals(reserved)) {
                return uid;
            }
        }
        throw new IllegalStateException("无法生成可用的录制UID");
    }

    private void withCourseLock(String courseId, Runnable action) {
        String lockKey = LOCK_KEY_PREFIX + courseId;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }
        try {
            action.run();
        } finally {
            stringRedisTemplate.execute(
                    RELEASE_LOCK_SCRIPT,
                    List.of(lockKey),
                    lockValue
            );
        }
    }

    private void updateRoomRecordingStatus(String roomId, int status) {
        try {
            Map<String, Object> properties = new LinkedHashMap<>();
            if (status == RECORDING_PAUSED || status == RECORDING_RUNNING) {
                properties.put("customRecordReady", 1);
            }
            properties.put("customRecordStatus", status);
            classroomClient.updateRoomProperties(roomId, properties);
        } catch (RuntimeException exception) {
            log.warn(
                    "更新声网教室录制状态失败，roomId={}，status={}，error={}",
                    roomId,
                    status,
                    exception.getMessage()
            );
        }
    }

    private List<Map<String, Object>> extractFiles(RecordingStatusResponse response) {
        if (response == null
                || response.serverResponse() == null
                || response.serverResponse().extensionServiceState() == null) {
            return List.of();
        }
        for (RecordingStatusResponse.ExtensionServiceState state
                : response.serverResponse().extensionServiceState()) {
            if (RECORDING_SERVICE_NAME.equals(state.serviceName()) && state.payload() != null) {
                return fileEntries(state.payload().get("fileList"));
            }
        }
        return List.of();
    }

    private List<Map<String, Object>> fileEntries(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<Map<String, Object>> files = new ArrayList<>();
        for (Object item : values) {
            Map<String, Object> file = map(item);
            if (file != null) {
                files.add(file);
            }
        }
        return files;
    }

    private List<String> readUrls(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JacksonException exception) {
            log.warn("无法解析直播课录像地址，error={}", exception.getMessage());
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("录制数据JSON序列化失败", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private String recordingFileBaseUrl() {
        String baseUrl = requireText(ossProperties.publicBaseUrl(), "OSS公开访问地址未配置");
        return (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "videorecord/mp4/";
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private String text(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
