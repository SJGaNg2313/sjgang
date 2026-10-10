package com.ai4.externalcrm.model;

/**
 * CRM 同步教师时需要保存的完整数据。
 */
public record CrmTeacherData(
        String id,
        Long userId,
        String username,
        String password,
        String nickname,
        Integer crmTeacherId,
        String bz,
        String avatar
) {
}
