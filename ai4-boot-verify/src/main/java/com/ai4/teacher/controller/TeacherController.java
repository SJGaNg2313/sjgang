package com.ai4.teacher.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.teacher.dto.TeacherListItemResponse;
import com.ai4.teacher.service.TeacherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 教师管理接口。 */
@RestController
public class TeacherController {
    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    /** 查询教师列表，账号和昵称均为可选模糊筛选条件。 */
    @GetMapping("/teachers")
    public ApiResponse<List<TeacherListItemResponse>> listTeachers(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String nickName
    ) {
        return ApiResponse.success(teacherService.listTeachers(userName, nickName));
    }
}
