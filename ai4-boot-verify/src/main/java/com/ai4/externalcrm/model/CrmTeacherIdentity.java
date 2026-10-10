package com.ai4.externalcrm.model;

/**
 * CRM 教师主键及其关联的后台用户 ID。
 */
public record CrmTeacherIdentity(
        String id,
        Long userId
) {
}
