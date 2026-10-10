package com.ai4.externalcrm.repository;

import com.ai4.externalcrm.dto.CrmCoursePackageStatResponse;
import com.ai4.externalcrm.model.CrmClassCreateData;
import com.ai4.externalcrm.model.CrmClassSchedule;

import java.util.List;
import java.util.Optional;

/** CRM 班级同步的数据访问边界。 */
public interface CrmClassRepository {

    Optional<String> findClassIdByCrmClassId(Integer crmClassId);

    Optional<String> findTeacherIdByCrmTeacherId(Integer crmTeacherId);

    Optional<CrmClassSchedule> findScheduleByCrmClassId(Integer crmClassId);

    void save(CrmClassCreateData data);

    int update(CrmClassCreateData data);

    int updateStatus(String classId, Integer classStatus);

    long findLiveCourseCountByPackageId(long packageId);

    List<CrmCoursePackageStatResponse> findCoursePackageStats();
}
