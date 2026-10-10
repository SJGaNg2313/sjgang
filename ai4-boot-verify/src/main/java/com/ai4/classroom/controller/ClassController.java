package com.ai4.classroom.controller;

import com.ai4.classroom.dto.ClassOptionResponse;
import com.ai4.classroom.service.ClassService;
import com.ai4.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 班级管理接口。 */
@RestController
@RequestMapping("/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /** 查询建课页面的班级下拉选项。 */
    @GetMapping("/options")
    public ApiResponse<List<ClassOptionResponse>> options() {
        return ApiResponse.success(classService.options());
    }
}
