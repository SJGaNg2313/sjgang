package com.ai4.externalcrm.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRM 班级学员同步的数据访问接口。
 */
public interface CrmClassStudentRepository {
    Map<String, String> findUserIdsByPhones(List<String> phones);
    Set<String> findActiveUserIdsByClassId(String classId);
    void bind(String bindingId, String classId, String userId);
    int remove(String classId, String userId);
}
