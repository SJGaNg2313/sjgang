package com.ai4.teacher.service;

import com.ai4.teacher.dto.TeacherListItemResponse;
import com.ai4.teacher.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** 教师管理业务。 */
@Service
public class TeacherService {
    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    /** 按账号、昵称可选模糊查询教师。 */
    public List<TeacherListItemResponse> listTeachers(String userName, String nickName) {
        return teacherRepository.findAll(normalize(userName), normalize(nickName));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
