package com.ai4.livecourse.service;

import com.ai4.classroom.model.ClassInfo;
import com.ai4.classroom.repository.ClassRepository;
import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.id.SnowflakeIdGenerator;
import com.ai4.common.response.PageResponse;
import com.ai4.livecourse.dto.CreateLiveCourseRequest;
import com.ai4.livecourse.dto.CreateLiveCourseResponse;
import com.ai4.livecourse.dto.LiveCourseDetailResponse;
import com.ai4.livecourse.dto.LiveCourseDetailStudentResponse;
import com.ai4.livecourse.dto.LiveCourseListItemResponse;
import com.ai4.livecourse.dto.LiveCoursePageQuery;
import com.ai4.livecourse.dto.LiveCourseStudentPageQuery;
import com.ai4.livecourse.dto.LiveCourseVideoResponse;
import com.ai4.livecourse.dto.StudentSubmissionFileResponse;
import com.ai4.externalcrm.event.CrmCoursePackageLiveCountChangedEvent;
import com.ai4.livecourse.model.LiveCourseAttendanceData;
import com.ai4.livecourse.model.LiveCourseCreateData;
import com.ai4.livecourse.model.LiveCourseDetailData;
import com.ai4.livecourse.repository.LiveCourseRepository;
import com.ai4.livecourse.support.RoomIdGenerator;
import com.ai4.project.model.Project;
import com.ai4.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** 直播课列表查询业务。 */
@Service
public class LiveCourseService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int ROOM_TYPE_LARGE_CLASS = 2;

    private final LiveCourseRepository liveCourseRepository;
    private final ClassRepository classRepository;
    private final ProjectRepository projectRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final RoomIdGenerator roomIdGenerator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public LiveCourseService(
            LiveCourseRepository liveCourseRepository,
            ClassRepository classRepository,
            ProjectRepository projectRepository,
            SnowflakeIdGenerator snowflakeIdGenerator,
            RoomIdGenerator roomIdGenerator,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.liveCourseRepository = liveCourseRepository;
        this.classRepository = classRepository;
        this.projectRepository = projectRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.roomIdGenerator = roomIdGenerator;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    /** 按条件分页查询有效直播课。 */
    @Transactional(readOnly = true)
    public PageResponse<LiveCourseListItemResponse> list(LiveCoursePageQuery query) {
        int pageNum = query.normalizedPageNum();
        int pageSize = query.normalizedPageSize();
        return new PageResponse<>(
                liveCourseRepository.findPage(query, query.offset(), pageSize),
                liveCourseRepository.count(query),
                pageNum,
                pageSize
        );
    }

    /**
     * 查询直播课详情，并对该班级的有效学员进行分页。
     */
    @Transactional(readOnly = true)
    public LiveCourseDetailResponse detail(
            String courseId,
            LiveCourseStudentPageQuery query
    ) {
        String normalizedCourseId = courseId == null ? "" : courseId.trim();
        LiveCourseDetailData detail = liveCourseRepository.findDetailById(normalizedCourseId)
                .orElseThrow(() -> new BizException(ErrorCode.LIVE_COURSE_NOT_FOUND));
        LiveCourseAttendanceData attendance = liveCourseRepository.findAttendanceById(normalizedCourseId);

        int pageNum = query.normalizedPageNum();
        int pageSize = query.normalizedPageSize();
        PageResponse<LiveCourseDetailStudentResponse> students = new PageResponse<>(
                liveCourseRepository.findDetailStudents(normalizedCourseId, query.offset(), pageSize),
                liveCourseRepository.countDetailStudents(normalizedCourseId),
                pageNum,
                pageSize
        );
        LocalDateTime courseTime = toLocalDateTime(detail.courseTime());

        return new LiveCourseDetailResponse(
                detail.courseId(),
                detail.roomId(),
                detail.className(),
                courseTime,
                detail.teacherName(),
                calculateActualDurationSeconds(detail),
                attendance.attendanceCount(),
                attendance.absenceCount(),
                attendance.lateCount(),
                attendance.earlyLeaveCount(),
                buildVideos(normalizedCourseId, courseTime, detail.className()),
                students
        );
    }

    /** 查询学员在当前直播课对应项目下历次提交的全部文件。 */
    @Transactional(readOnly = true)
    public List<StudentSubmissionFileResponse> listStudentSubmissionFiles(
            String courseId,
            String studentId
    ) {
        String normalizedCourseId = courseId == null ? "" : courseId.trim();
        String normalizedStudentId = studentId == null ? "" : studentId.trim();
        return liveCourseRepository.findStudentSubmissionFiles(
                normalizedCourseId,
                normalizedStudentId
        );
    }

    /** 创建直播课并建立班级、项目关联。 */
    @Transactional
    public CreateLiveCourseResponse create(CreateLiveCourseRequest request) {
        String classId = request.classId().trim();
        ClassInfo classInfo = classRepository.findActiveById(classId)
                .orElseThrow(() -> new BizException(ErrorCode.CLASS_NOT_FOUND));
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new BizException(ErrorCode.PROJECT_NOT_FOUND));

        if (!Objects.equals(classInfo.packageId(), project.packageId())) {
            throw new BizException(ErrorCode.LIVE_COURSE_PACKAGE_MISMATCH);
        }
        if (classInfo.teacherId() == null
                || classInfo.teacherId().isBlank()) {
            throw new BizException(ErrorCode.CLASS_CONFIGURATION_INVALID);
        }

        long courseTime = LocalDateTime.of(request.courseDate(), request.courseStartTime())
                .atZone(BUSINESS_ZONE)
                .toInstant()
                .toEpochMilli();
        String courseId = snowflakeIdGenerator.nextId();
        String roomId = roomIdGenerator.nextRoomId();

        liveCourseRepository.save(new LiveCourseCreateData(
                courseId,
                courseTime,
                request.courseDuration(),
                classInfo.teacherId(),
                classInfo.closeDelay() == null ? 0 : classInfo.closeDelay(),
                1,
                classInfo.record() == null ? 1 : classInfo.record(),
                roomId,
                ROOM_TYPE_LARGE_CLASS
        ));
        liveCourseRepository.bindToClassAndProject(classId, project.id(), courseId);
        eventPublisher.publishEvent(
                new CrmCoursePackageLiveCountChangedEvent(classInfo.packageId())
        );

        return new CreateLiveCourseResponse(courseId, roomId);
    }

    /** 仅逻辑删除未开始的直播课，并解除班级、项目关联。 */
    @Transactional
    public void delete(String courseId) {
        String normalizedCourseId = courseId == null ? "" : courseId.trim();
        Long packageId = liveCourseRepository.findPackageIdByCourseId(normalizedCourseId)
                .orElse(null);
        int affectedRows = liveCourseRepository.softDeleteUnstartedById(normalizedCourseId);
        if (affectedRows == 0) {
            if (liveCourseRepository.findStatusById(normalizedCourseId).isEmpty()) {
                throw new BizException(ErrorCode.LIVE_COURSE_NOT_FOUND);
            }
            throw new BizException(ErrorCode.LIVE_COURSE_DELETE_FORBIDDEN);
        }
        liveCourseRepository.deleteRelationByCourseId(normalizedCourseId);
        if (packageId != null) {
            eventPublisher.publishEvent(new CrmCoursePackageLiveCountChangedEvent(packageId));
        }
    }

    private long calculateActualDurationSeconds(LiveCourseDetailData detail) {
        Long startTime = detail.startTime();
        if (startTime == null || startTime <= 0) {
            return 0;
        }

        Long endTime = detail.endTime();
        long effectiveEndTime;
        if (endTime != null && endTime >= startTime) {
            effectiveEndTime = endTime;
        } else if (detail.status() == 1 || detail.status() == 2) {
            effectiveEndTime = System.currentTimeMillis();
        } else {
            return 0;
        }
        return Math.max(0, effectiveEndTime - startTime) / 1000;
    }

    private List<LiveCourseVideoResponse> buildVideos(
            String courseId,
            LocalDateTime courseTime,
            String className
    ) {
        List<String> urls = liveCourseRepository.findVideoUrlsJson(courseId)
                .map(this::parseVideoUrls)
                .orElseGet(List::of);
        String date = courseTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String normalizedClassName = className == null ? "" : className;

        return java.util.stream.IntStream.range(0, urls.size())
                .mapToObj(index -> new LiveCourseVideoResponse(
                        date + normalizedClassName + "-" + (index + 1),
                        urls.get(index)
                ))
                .toList();
    }

    private List<String> parseVideoUrls(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        String normalized = rawValue.trim();
        if (!normalized.startsWith("[")) {
            return List.of(normalized);
        }

        try {
            List<String> urls = objectMapper.readValue(normalized, new TypeReference<>() { });
            return urls.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(url -> !url.isEmpty())
                    .distinct()
                    .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("直播课录像地址格式错误", exception);
        }
    }

    private LocalDateTime toLocalDateTime(long epochMilli) {
        return java.time.Instant.ofEpochMilli(epochMilli)
                .atZone(BUSINESS_ZONE)
                .toLocalDateTime();
    }
}
