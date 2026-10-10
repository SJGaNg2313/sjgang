package com.ai4.externalcrm.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.id.SnowflakeIdGenerator;
import com.ai4.externalcrm.dto.CreateCrmClassRequest;
import com.ai4.externalcrm.dto.CrmCoursePackageStatResponse;
import com.ai4.externalcrm.dto.UpdateCrmClassRequest;
import com.ai4.externalcrm.event.CrmCoursePackageLiveCountChangedEvent;
import com.ai4.externalcrm.model.CrmClassCreateData;
import com.ai4.externalcrm.model.CrmClassSchedule;
import com.ai4.externalcrm.repository.CrmClassRepository;
import com.ai4.livecourse.model.LiveCourseCreateData;
import com.ai4.livecourse.repository.LiveCourseRepository;
import com.ai4.livecourse.support.RoomIdGenerator;
import com.ai4.project.model.Project;
import com.ai4.project.repository.ProjectRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** 接收 CRM 班级数据并维护对应直播课。 */
@Service
public class CrmClassSyncService {

    private static final int CLASS_ACTIVE = 1;
    private static final int CLASS_STOPPED = 2;
    private static final int ROOM_TYPE_LARGE_CLASS = 2;
    private static final int SYSTEM_CREATED = 0;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter HOUR_MINUTE_SECOND = DateTimeFormatter.ofPattern("H:mm:ss");

    private final CrmClassRepository crmClassRepository;
    private final ProjectRepository projectRepository;
    private final LiveCourseRepository liveCourseRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final RoomIdGenerator roomIdGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public CrmClassSyncService(
            CrmClassRepository crmClassRepository,
            ProjectRepository projectRepository,
            LiveCourseRepository liveCourseRepository,
            SnowflakeIdGenerator snowflakeIdGenerator,
            RoomIdGenerator roomIdGenerator,
            ApplicationEventPublisher eventPublisher
    ) {
        this.crmClassRepository = crmClassRepository;
        this.projectRepository = projectRepository;
        this.liveCourseRepository = liveCourseRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.roomIdGenerator = roomIdGenerator;
        this.eventPublisher = eventPublisher;
    }

    /** 新增 CRM 班级；正常班级同步创建该课包下全部项目的直播课。 */
    @Transactional
    public String create(CreateCrmClassRequest request) {
        validateStatus(request.classStatus());
        validateRecord(request.record());

        LocalTime startTime = parseTime(request.startTime());
        LocalTime endTime = parseTime(request.endTime());
        int courseDuration = calculateDuration(startTime, endTime);
        long packageId = parseLong(request.packageId());
        int crmTeacherId = parseInteger(request.teacherId());

        String existingClassId = crmClassRepository.findClassIdByCrmClassId(request.classId())
                .orElse(null);
        if (existingClassId != null) {
            return existingClassId;
        }

        String teacherId = crmClassRepository.findTeacherIdByCrmTeacherId(crmTeacherId)
                .orElseThrow(() -> new BizException(ErrorCode.CRM_TEACHER_NOT_FOUND));
        List<Project> projects = findProjects(packageId);

        String classId = snowflakeIdGenerator.nextId();
        LocalDateTime firstCourseTime = LocalDateTime.of(request.startClassTime(), startTime);
        CrmClassCreateData classData = new CrmClassCreateData(
                classId,
                request.classId(),
                request.className().trim(),
                startTime,
                endTime,
                packageId,
                teacherId,
                0,
                ROOM_TYPE_LARGE_CLASS,
                request.record(),
                request.courseRule(),
                firstCourseTime,
                request.classStatus(),
                request.closeDelay(),
                0
        );
        crmClassRepository.save(classData);

        if (request.classStatus() == CLASS_ACTIVE) {
            scheduleCourses(toSchedule(classData), projects, courseDuration);
            publishPackageLiveCountChanged(packageId);
        }
        return classId;
    }

    /**
     * 修改班级信息，保留已开始的直播课并重建未开始的直播课。
     */
    @Transactional
    public String update(UpdateCrmClassRequest request) {
        validateStatus(request.classStatus());
        validateRecord(request.record());

        CrmClassSchedule current = crmClassRepository.findScheduleByCrmClassId(request.classId())
                .orElseThrow(() -> new BizException(ErrorCode.CRM_CLASS_NOT_FOUND));
        LocalTime startTime = parseTime(request.startTime());
        LocalTime endTime = parseTime(request.endTime());
        int courseDuration = calculateDuration(startTime, endTime);
        long packageId = parseLong(request.packageId());
        int crmTeacherId = parseInteger(request.teacherId());
        String teacherId = crmClassRepository.findTeacherIdByCrmTeacherId(crmTeacherId)
                .orElseThrow(() -> new BizException(ErrorCode.CRM_TEACHER_NOT_FOUND));

        CrmClassCreateData classData = new CrmClassCreateData(
                current.classId(),
                request.classId(),
                request.className().trim(),
                startTime,
                endTime,
                packageId,
                teacherId,
                0,
                ROOM_TYPE_LARGE_CLASS,
                request.record(),
                request.courseRule(),
                LocalDateTime.of(request.startClassTime(), startTime),
                request.classStatus(),
                request.closeDelay(),
                0
        );
        crmClassRepository.update(classData);

        List<String> unstartedCourseIds = liveCourseRepository
                .findUnstartedCourseIdsByClassId(current.classId());
        liveCourseRepository.softDeleteByIds(unstartedCourseIds);
        liveCourseRepository.deleteRelationsByCourseIds(current.classId(), unstartedCourseIds);

        if (request.classStatus() == CLASS_ACTIVE) {
            scheduleCourses(toSchedule(classData), findProjects(packageId), courseDuration);
        }
        publishPackageLiveCountChanged(current.packageId());
        if (current.packageId() != packageId) {
            publishPackageLiveCountChanged(packageId);
        }
        return current.classId();
    }

    /** CRM 停课时取消未开始课程，恢复时按原排课配置补建缺失课程。 */
    @Transactional
    public String updateStatus(Integer crmClassId, Integer classStatus) {
        validateStatus(classStatus);
        CrmClassSchedule schedule = crmClassRepository.findScheduleByCrmClassId(crmClassId)
                .orElseThrow(() -> new BizException(ErrorCode.CRM_CLASS_NOT_FOUND));
        if (classStatus.equals(schedule.classStatus())) {
            return schedule.classId();
        }

        crmClassRepository.updateStatus(schedule.classId(), classStatus);
        if (classStatus == CLASS_STOPPED) {
            List<String> courseIds = liveCourseRepository
                    .findUnstartedCourseIdsByClassId(schedule.classId());
            liveCourseRepository.softDeleteByIds(courseIds);
            liveCourseRepository.deleteRelationsByCourseIds(schedule.classId(), courseIds);
            publishPackageLiveCountChanged(schedule.packageId());
            return schedule.classId();
        }

        validateSchedule(schedule);
        scheduleCourses(
                schedule,
                findProjects(schedule.packageId()),
                calculateDuration(schedule.startTime(), schedule.endTime())
        );
        publishPackageLiveCountChanged(schedule.packageId());
        return schedule.classId();
    }

    /**
     * 查询每个有效课包及其现有有效直播课数量。
     */
    public List<CrmCoursePackageStatResponse> findCoursePackageStats() {
        return crmClassRepository.findCoursePackageStats();
    }

    private void scheduleCourses(
            CrmClassSchedule schedule,
            List<Project> projects,
            int courseDuration
    ) {
        LocalDateTime firstCourseTime = LocalDateTime.of(
                schedule.startClassTime().toLocalDate(),
                schedule.startTime()
        );
        for (int index = 0; index < projects.size(); index++) {
            Project project = projects.get(index);
            if (liveCourseRepository.existsActiveByClassAndProject(
                    schedule.classId(),
                    project.id()
            )) {
                continue;
            }

            long courseTime = firstCourseTime
                    .plusDays((long) index * schedule.courseRule())
                    .atZone(BUSINESS_ZONE)
                    .toInstant()
                    .toEpochMilli();
            String courseId = snowflakeIdGenerator.nextId();
            liveCourseRepository.save(new LiveCourseCreateData(
                    courseId,
                    courseTime,
                    courseDuration,
                    schedule.teacherId(),
                    schedule.closeDelay() == null ? 0 : schedule.closeDelay(),
                    SYSTEM_CREATED,
                    schedule.record() == null ? 1 : schedule.record(),
                    roomIdGenerator.nextRoomId(),
                    schedule.roomType() == null ? ROOM_TYPE_LARGE_CLASS : schedule.roomType()
            ));
            liveCourseRepository.bindToClassAndProject(
                    schedule.classId(),
                    project.id(),
                    courseId
            );
        }
    }

    private CrmClassSchedule toSchedule(CrmClassCreateData data) {
        return new CrmClassSchedule(
                data.classId(),
                data.packageId(),
                data.startTime(),
                data.endTime(),
                data.startClassTime(),
                data.courseRule(),
                data.teacherId(),
                data.roomType(),
                data.record(),
                data.closeDelay(),
                data.classStatus()
        );
    }

    private List<Project> findProjects(Long packageId) {
        List<Project> projects = projectRepository.findByPackageId(packageId);
        if (projects.isEmpty()) {
            throw new BizException(ErrorCode.CRM_PACKAGE_PROJECT_EMPTY);
        }
        return projects;
    }

    private void validateSchedule(CrmClassSchedule schedule) {
        if (schedule.startTime() == null
                || schedule.endTime() == null
                || schedule.startClassTime() == null
                || schedule.courseRule() == null
                || schedule.courseRule() <= 0
                || schedule.teacherId() == null
                || schedule.teacherId().isBlank()) {
            throw new BizException(ErrorCode.CRM_CLASS_CONFIGURATION_INVALID);
        }
    }

    private LocalTime parseTime(String value) {
        String time = value.trim();
        try {
            return LocalTime.parse(time, HOUR_MINUTE);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalTime.parse(time, HOUR_MINUTE_SECOND);
            } catch (DateTimeParseException exception) {
                throw new BizException(ErrorCode.CRM_CLASS_CONFIGURATION_INVALID);
            }
        }
    }

    private int calculateDuration(LocalTime startTime, LocalTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0 || minutes > Integer.MAX_VALUE) {
            throw new BizException(ErrorCode.CRM_CLASS_CONFIGURATION_INVALID);
        }
        return (int) minutes;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.CRM_CLASS_CONFIGURATION_INVALID);
        }
    }

    private int parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.CRM_CLASS_CONFIGURATION_INVALID);
        }
    }

    private void validateStatus(Integer classStatus) {
        if (classStatus != CLASS_ACTIVE && classStatus != CLASS_STOPPED) {
            throw new BizException(ErrorCode.CRM_CLASS_STATUS_INVALID);
        }
    }

    private void validateRecord(Integer record) {
        if (record != 1 && record != 2) {
            throw new BizException(ErrorCode.CRM_RECORD_INVALID);
        }
    }

    private void publishPackageLiveCountChanged(long packageId) {
        eventPublisher.publishEvent(new CrmCoursePackageLiveCountChangedEvent(packageId));
    }
}
