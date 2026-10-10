package com.ai4.externalcrm.service;

import com.ai4.externalcrm.client.CrmLiveCourseClient;
import com.ai4.externalcrm.config.CrmCallbackProperties;
import com.ai4.externalcrm.dto.CrmClassEndCallbackRequest;
import com.ai4.externalcrm.dto.CrmClassEndStudentRequest;
import com.ai4.externalcrm.event.CrmCoursePackageLiveCountChangedEvent;
import com.ai4.externalcrm.model.CrmCourseEndData;
import com.ai4.externalcrm.model.CrmStudentAttendanceData;
import com.ai4.externalcrm.repository.CrmClassRepository;
import com.ai4.externalcrm.repository.CrmLiveCourseCallbackRepository;
import com.ai4.livecourse.agora.event.AgoraRecordingCompletedEvent;
import com.ai4.livecourse.agora.event.AgoraRoomClosedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.core.annotation.Order;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/** 同步课包直播课数量和直播课结束考勤到 CRM。 */
@Component
public class CrmLiveCourseSyncService {

    private static final Logger log = LoggerFactory.getLogger(CrmLiveCourseSyncService.class);

    private static final int CLOSED_STATUS = 3;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter CALLBACK_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Duration CALLBACK_LOCK_TTL = Duration.ofSeconds(60);
    private static final Duration CALLBACK_SENT_TTL = Duration.ofDays(30);
    private static final String CALLBACK_LOCK_PREFIX =
            "ai4:external:crm:class-end-callback:lock:";
    private static final String CALLBACK_SENT_PREFIX =
            "ai4:external:crm:class-end-callback:sent:";
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] "
                            + "then return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class
            );

    private final CrmCallbackProperties properties;
    private final CrmClassRepository crmClassRepository;
    private final CrmLiveCourseCallbackRepository courseRepository;
    private final CrmLiveCourseClient crmClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService callbackExecutor;

    public CrmLiveCourseSyncService(
            CrmCallbackProperties properties,
            CrmClassRepository crmClassRepository,
            CrmLiveCourseCallbackRepository courseRepository,
            CrmLiveCourseClient crmClient,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Qualifier("crmCallbackExecutor") ExecutorService callbackExecutor
    ) {
        this.properties = properties;
        this.crmClassRepository = crmClassRepository;
        this.courseRepository = courseRepository;
        this.crmClient = crmClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.callbackExecutor = callbackExecutor;
    }

    /** 数据库事务提交后异步同步课包直播课数量。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePackageLiveCountChanged(CrmCoursePackageLiveCountChangedEvent event) {
        submitAsync(
                "同步课包直播课数量",
                () -> syncPackageLiveCount(event.packageId())
        );
    }

    /** 课堂结束事务提交后尝试回调；录制尚未完成时由录制完成事件继续触发。 */
    @Order(100)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoomClosed(AgoraRoomClosedEvent event) {
        submitAsync(
                "回调班级直播课结束数据",
                () -> tryCallbackCourseEnd(event.courseId())
        );
    }

    /** 云端录制完成并写入回放地址后再次尝试回调。 */
    @EventListener
    public void handleRecordingCompleted(AgoraRecordingCompletedEvent event) {
        submitAsync(
                "录制完成后回调班级直播课结束数据",
                () -> tryCallbackCourseEnd(event.courseId())
        );
    }

    /** 定时扫描最近关闭的课程，补偿 CRM 暂时不可用或异步事件失败的情况。 */
    @Scheduled(fixedDelayString = "${ai4.external.crm.callback.retry-scan-delay-ms:60000}")
    public void retryClosedCourseCallbacks() {
        if (!isReady()) {
            return;
        }
        long startTime = System.currentTimeMillis()
                - Duration.ofDays(Math.max(1, properties.retryHistoryDays())).toMillis();
        for (String courseId : courseRepository.findClosedCourseIds(startTime, 100)) {
            submitAsync(
                    "补偿回调班级直播课结束数据",
                    () -> tryCallbackCourseEnd(courseId)
            );
        }
    }

    private void syncPackageLiveCount(long packageId) {
        if (!isReady()) {
            return;
        }
        long count = crmClassRepository.findLiveCourseCountByPackageId(packageId);
        try {
            crmClient.updateCoursePackageLiveCount(packageId, count);
            log.info(
                    "课包直播课数量已同步到CRM，packageId={}，liveLessonCount={}",
                    packageId,
                    count
            );
        } catch (RuntimeException exception) {
            log.error(
                    "课包直播课数量同步CRM失败，packageId={}，liveLessonCount={}",
                    packageId,
                    count,
                    exception
            );
        }
    }

    private void tryCallbackCourseEnd(String courseId) {
        if (!isReady()) {
            return;
        }

        CrmCourseEndData course = courseRepository.findCourse(courseId).orElse(null);
        if (course == null || course.status() != CLOSED_STATUS) {
            return;
        }
        if (courseRepository.existsActiveRecording(courseId)) {
            log.info("CRM结束回调等待云端录制完成，courseId={}", courseId);
            return;
        }

        String sentKey = CALLBACK_SENT_PREFIX + courseId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(sentKey))) {
            return;
        }

        String lockKey = CALLBACK_LOCK_PREFIX + courseId;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                lockValue,
                CALLBACK_LOCK_TTL
        );
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }

        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(sentKey))) {
                return;
            }
            CrmClassEndCallbackRequest request = buildCourseEndRequest(course);
            crmClient.callbackClassEnd(request);
            stringRedisTemplate.opsForValue().set(sentKey, "1", CALLBACK_SENT_TTL);
            log.info(
                    "班级直播课结束数据已同步到CRM，courseId={}，liveclassid={}，studentCount={}",
                    courseId,
                    course.crmClassId(),
                    request.list().size()
            );
        } catch (RuntimeException exception) {
            log.error("班级直播课结束数据同步CRM失败，courseId={}", courseId, exception);
        } finally {
            stringRedisTemplate.execute(
                    RELEASE_LOCK_SCRIPT,
                    List.of(lockKey),
                    lockValue
            );
        }
    }

    private CrmClassEndCallbackRequest buildCourseEndRequest(CrmCourseEndData course) {
        List<CrmClassEndStudentRequest> students = courseRepository
                .findStudentAttendances(course.courseId())
                .stream()
                .map(student -> toStudentRequest(student, course))
                .toList();
        if (students.isEmpty()) {
            throw new IllegalStateException("直播课没有可回调的有效学员");
        }
        return new CrmClassEndCallbackRequest(
                course.crmClassId(),
                Instant.ofEpochMilli(course.courseTime())
                        .atZone(BUSINESS_ZONE)
                        .toLocalDate()
                        .toString(),
                firstVideoUrl(course.m3u8UrlsJson()),
                students
        );
    }

    private CrmClassEndStudentRequest toStudentRequest(
            CrmStudentAttendanceData student,
            CrmCourseEndData course
    ) {
        long attendanceDuration = calculateAttendanceDuration(student, course);
        return new CrmClassEndStudentRequest(
                student.phone(),
                attendanceDuration,
                student.attendance(),
                student.late(),
                student.leaveEarly(),
                formatTime(student.joinTime()),
                formatTime(student.leaveTime())
        );
    }

    private long calculateAttendanceDuration(
            CrmStudentAttendanceData student,
            CrmCourseEndData course
    ) {
        if (student.attendance() != 1 || student.joinTime() == null) {
            return 0;
        }
        long scheduledEndTime = course.courseTime()
                + course.courseDuration() * 60_000L;
        long leaveTime = student.leaveTime() == null
                ? scheduledEndTime
                : Math.min(student.leaveTime(), scheduledEndTime);
        return Math.max(0, leaveTime - student.joinTime()) / 1000;
    }

    private String firstVideoUrl(String rawUrls) {
        if (rawUrls == null || rawUrls.isBlank()) {
            return "";
        }
        String normalized = rawUrls.trim();
        if (!normalized.startsWith("[")) {
            return normalized;
        }
        try {
            List<String> urls = objectMapper.readValue(
                    normalized,
                    new TypeReference<List<String>>() { }
            );
            return urls.stream()
                    .filter(url -> url != null && !url.isBlank())
                    .map(String::trim)
                    .findFirst()
                    .orElse("");
        } catch (JacksonException exception) {
            log.warn("CRM结束回调的回放地址格式错误，原始值={}", rawUrls);
            return "";
        }
    }

    private String formatTime(Long timestamp) {
        return timestamp == null
                ? null
                : Instant.ofEpochMilli(timestamp)
                .atZone(BUSINESS_ZONE)
                .format(CALLBACK_TIME);
    }

    private void submitAsync(String operation, Runnable task) {
        try {
            callbackExecutor.execute(() -> {
                try {
                    task.run();
                } catch (RuntimeException exception) {
                    log.error("{}执行失败", operation, exception);
                }
            });
        } catch (RuntimeException exception) {
            log.error("{}提交线程池失败", operation, exception);
        }
    }

    private boolean isReady() {
        if (!properties.enabled()) {
            return false;
        }
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            log.error("CRM出站同步已启用，但CRM基础地址未配置");
            return false;
        }
        return true;
    }
}
