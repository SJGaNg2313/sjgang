package com.ai4.course.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.course.model.CoursePackage;
import com.ai4.course.service.CoursePackageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 唯一课包查询接口。 */
@RestController
@RequestMapping("/course-package")
public class CoursePackageController {

    private final CoursePackageService coursePackageService;

    public CoursePackageController(CoursePackageService coursePackageService) {
        this.coursePackageService = coursePackageService;
    }

    /** 查询数据库中唯一的有效课包，不依赖固定课包 ID。 */
    @GetMapping
    public ApiResponse<CoursePackage> get() {
        return ApiResponse.success(coursePackageService.getOnly());
    }
}
