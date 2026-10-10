package com.ai4.externalcrm.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.externalcrm.dto.CreateCrmClassRequest;
import com.ai4.externalcrm.dto.BindCrmClassStudentsResponse;
import com.ai4.externalcrm.dto.CrmClassStudentsRequest;
import com.ai4.externalcrm.dto.CrmCoursePackageStatResponse;
import com.ai4.externalcrm.dto.RemoveCrmClassStudentsResponse;
import com.ai4.externalcrm.dto.UpdateCrmClassRequest;
import com.ai4.externalcrm.service.CrmClassStudentService;
import com.ai4.externalcrm.service.CrmClassSyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CRM 班级同步接口。 */
@RestController
@RequestMapping("/external/aiClass")
public class ExternalCrmClassController {

    private final CrmClassSyncService crmClassSyncService;
    private final CrmClassStudentService crmClassStudentService;

    public ExternalCrmClassController(
            CrmClassSyncService crmClassSyncService,
            CrmClassStudentService crmClassStudentService
    ) {
        this.crmClassSyncService = crmClassSyncService;
        this.crmClassStudentService = crmClassStudentService;
    }

    /** CRM 新增班级，并为正常班级自动创建项目直播课。 */
    @PostMapping("/add")
    public ApiResponse<String> create(@Valid @RequestBody CreateCrmClassRequest request) {
        return ApiResponse.success(crmClassSyncService.create(request));
    }

    /**
     * CRM 修改班级配置，并只重建未开始的直播课。
     */
    @PutMapping("/update")
    public ApiResponse<String> update(@Valid @RequestBody UpdateCrmClassRequest request) {
        return ApiResponse.success(crmClassSyncService.update(request));
    }

    /** CRM 开课或停课。 */
    @PostMapping("/openClose")
    public ApiResponse<String> updateStatus(
            @RequestParam Integer classId,
            @RequestParam Integer classStatus
    ) {
        return ApiResponse.success(crmClassSyncService.updateStatus(classId, classStatus));
    }

    /**
     * 根据手机号将已注册学员绑定到班级。
     */
    @PostMapping("/syncStudents")
    public ApiResponse<BindCrmClassStudentsResponse> bindStudents(
            @Valid @RequestBody CrmClassStudentsRequest request
    ) {
        return ApiResponse.success(crmClassStudentService.bind(request));
    }

    /**
     * 根据手机号将学员从班级中逻辑移除。
     */
    @PostMapping("/removeStudents")
    public ApiResponse<RemoveCrmClassStudentsResponse> removeStudents(
            @Valid @RequestBody CrmClassStudentsRequest request
    ) {
        return ApiResponse.success(crmClassStudentService.remove(request));
    }

    /**
     * 查询课包名称及其现有有效直播课数量。
     */
    @GetMapping("/coursePackageLiveStat")
    public ApiResponse<List<CrmCoursePackageStatResponse>> coursePackageLiveStat() {
        return ApiResponse.success(crmClassSyncService.findCoursePackageStats());
    }
}
