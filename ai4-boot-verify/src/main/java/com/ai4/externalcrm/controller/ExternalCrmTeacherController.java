package com.ai4.externalcrm.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.externalcrm.dto.CreateCrmTeacherRequest;
import com.ai4.externalcrm.dto.UpdateCrmTeacherRequest;
import com.ai4.externalcrm.service.CrmTeacherService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 教师同步接口。
 */
@RestController
@RequestMapping("/external/liveTeach")
public class ExternalCrmTeacherController {
    private final CrmTeacherService teacherService;

    public ExternalCrmTeacherController(CrmTeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/add")
    public ApiResponse<Void> create(@Valid @RequestBody CreateCrmTeacherRequest request) {
        teacherService.create(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/update")
    public ApiResponse<Void> update(@Valid @RequestBody UpdateCrmTeacherRequest request) {
        teacherService.update(request);
        return ApiResponse.success(null);
    }
}
