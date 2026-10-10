package com.ai4.teacher.dto;

/** 后台教师列表项。 */
public record TeacherListItemResponse(
        String id,
        Long userId,
        String userName,
        String nickName
) {
}
