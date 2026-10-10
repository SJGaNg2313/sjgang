package com.ai4.externalcrm.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.common.id.SnowflakeIdGenerator;
import com.ai4.externalcrm.dto.BindCrmClassStudentsResponse;
import com.ai4.externalcrm.dto.CrmClassStudentsRequest;
import com.ai4.externalcrm.dto.RemoveCrmClassStudentsResponse;
import com.ai4.externalcrm.repository.CrmClassRepository;
import com.ai4.externalcrm.repository.CrmClassStudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 同步 CRM 学员与本地班级的绑定关系。
 */
@Service
public class CrmClassStudentService {
    private final CrmClassRepository crmClassRepository;
    private final CrmClassStudentRepository studentRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public CrmClassStudentService(
            CrmClassRepository crmClassRepository,
            CrmClassStudentRepository studentRepository,
            SnowflakeIdGenerator snowflakeIdGenerator
    ) {
        this.crmClassRepository = crmClassRepository;
        this.studentRepository = studentRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Transactional
    public BindCrmClassStudentsResponse bind(CrmClassStudentsRequest request) {
        String classId = findClassId(request.classId());
        List<String> phones = normalizePhones(request.phoneList());
        Map<String, String> userIds = findAllUsers(phones);
        Set<String> activeUserIds = studentRepository.findActiveUserIdsByClassId(classId);
        int addedCount = 0;
        int alreadyBoundCount = 0;
        for (String phone : phones) {
            String userId = userIds.get(phone);
            if (activeUserIds.contains(userId)) {
                alreadyBoundCount++;
                continue;
            }
            studentRepository.bind(snowflakeIdGenerator.nextId(), classId, userId);
            activeUserIds.add(userId);
            addedCount++;
        }
        return new BindCrmClassStudentsResponse(addedCount, alreadyBoundCount);
    }

    @Transactional
    public RemoveCrmClassStudentsResponse remove(CrmClassStudentsRequest request) {
        String classId = findClassId(request.classId());
        List<String> phones = normalizePhones(request.phoneList());
        Map<String, String> userIds = findAllUsers(phones);
        int removedCount = 0;
        int notInClassCount = 0;
        for (String phone : phones) {
            if (studentRepository.remove(classId, userIds.get(phone)) > 0) {
                removedCount++;
            } else {
                notInClassCount++;
            }
        }
        return new RemoveCrmClassStudentsResponse(removedCount, notInClassCount);
    }

    private String findClassId(Integer crmClassId) {
        return crmClassRepository.findClassIdByCrmClassId(crmClassId)
                .orElseThrow(() -> new BizException(ErrorCode.CRM_CLASS_NOT_FOUND));
    }

    private List<String> normalizePhones(List<String> phoneList) {
        LinkedHashSet<String> phones = new LinkedHashSet<>();
        for (String phone : phoneList) {
            if (phone != null && !phone.isBlank()) {
                phones.add(phone.trim());
            }
        }
        if (phones.isEmpty()) {
            throw new BizException(ErrorCode.CRM_STUDENT_LIST_INVALID);
        }
        return new ArrayList<>(phones);
    }

    private Map<String, String> findAllUsers(List<String> phones) {
        Map<String, String> userIds = studentRepository.findUserIdsByPhones(phones);
        List<String> missingPhones = phones.stream()
                .filter(phone -> !userIds.containsKey(phone))
                .toList();
        if (!missingPhones.isEmpty()) {
            throw new BizException(
                    ErrorCode.CRM_STUDENT_NOT_FOUND.code(),
                    "学员不存在，手机号：" + String.join(",", missingPhones)
            );
        }
        return userIds;
    }
}
