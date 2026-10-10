package com.ai4.teacher.repository;

import com.ai4.teacher.dto.TeacherListItemResponse;

import java.util.List;

/** 教师管理数据访问接口。 */
public interface TeacherRepository {
    List<TeacherListItemResponse> findAll(String userName, String nickName);
}
