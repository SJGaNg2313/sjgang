package com.ai4.livecourse.repository;

import com.ai4.livecourse.dto.LiveCourseListItemResponse;
import com.ai4.livecourse.dto.LiveCourseDetailStudentResponse;
import com.ai4.livecourse.dto.LiveCoursePageQuery;
import com.ai4.livecourse.dto.StudentSubmissionFileResponse;
import com.ai4.livecourse.model.LiveCourseAttendanceData;
import com.ai4.livecourse.model.LiveCourseCreateData;
import com.ai4.livecourse.model.LiveCourseDetailData;
import com.ai4.livecourse.model.AgoraRoomProvisionData;

import java.util.List;
import java.util.Optional;

/** 直播课列表数据访问接口。 */
public interface LiveCourseRepository {

    long count(LiveCoursePageQuery query);

    List<LiveCourseListItemResponse> findPage(
            LiveCoursePageQuery query,
            int offset,
            int limit
    );

    Optional<LiveCourseDetailData> findDetailById(String courseId);

    LiveCourseAttendanceData findAttendanceById(String courseId);

    long countDetailStudents(String courseId);

    List<LiveCourseDetailStudentResponse> findDetailStudents(
            String courseId,
            int offset,
            int limit
    );

    List<StudentSubmissionFileResponse> findStudentSubmissionFiles(
            String courseId,
            String studentId
    );

    Optional<String> findVideoUrlsJson(String courseId);

    List<AgoraRoomProvisionData> findRoomsNeedingProvisioning(
            long currentTime,
            long latestCourseTime
    );

    void save(LiveCourseCreateData course);

    void bindToClassAndProject(String classId, Long projectId, String courseId);

    Optional<Long> findPackageIdByCourseId(String courseId);

    boolean existsActiveByClassAndProject(String classId, Long projectId);

    List<String> findUnstartedCourseIdsByClassId(String classId);

    void softDeleteByIds(List<String> courseIds);

    void deleteRelationsByCourseIds(String classId, List<String> courseIds);

    Optional<Integer> findStatusById(String courseId);

    int softDeleteUnstartedById(String courseId);

    void deleteRelationByCourseId(String courseId);
}
