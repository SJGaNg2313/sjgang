package com.ai4.externalcrm.repository;

import com.ai4.externalcrm.model.CrmCourseEndData;
import com.ai4.externalcrm.model.CrmStudentAttendanceData;

import java.util.List;
import java.util.Optional;

/** CRM 直播课回调所需的数据访问边界。 */
public interface CrmLiveCourseCallbackRepository {

    Optional<CrmCourseEndData> findCourse(String courseId);

    List<CrmStudentAttendanceData> findStudentAttendances(String courseId);

    boolean existsActiveRecording(String courseId);

    List<String> findClosedCourseIds(long startTime, int limit);
}
