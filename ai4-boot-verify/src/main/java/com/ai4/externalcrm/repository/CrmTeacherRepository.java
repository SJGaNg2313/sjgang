package com.ai4.externalcrm.repository;

import com.ai4.externalcrm.model.CrmTeacherData;
import com.ai4.externalcrm.model.CrmTeacherIdentity;

import java.util.Optional;

/**
 * CRM 教师同步的数据访问接口。
 */
public interface CrmTeacherRepository {
    Optional<CrmTeacherIdentity> findByCrmTeacherId(Integer crmTeacherId);
    Optional<CrmTeacherIdentity> findByUserId(Long userId);
    boolean usernameExists(String username, Integer excludedCrmTeacherId);
    void save(CrmTeacherData teacher);
    int update(CrmTeacherData teacher);
}
