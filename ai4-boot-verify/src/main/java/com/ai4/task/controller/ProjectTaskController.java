package com.ai4.task.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.task.dto.*;
import com.ai4.task.service.ProjectTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 项目领取任务配置接口 */
@RestController
@RequestMapping("/projects/{projectId}/task")
public class ProjectTaskController {

    private final ProjectTaskService taskService;

    public ProjectTaskController(ProjectTaskService taskService) {
        this.taskService = taskService;
    }

    /** 查询指定项目的领取任务配置 */
    @GetMapping
    public ApiResponse<ProjectTaskResponse> get(@PathVariable Long projectId) {
        return ApiResponse.success(taskService.get(projectId));
    }

    /** 创建指定项目的领取任务配置 */
    @PostMapping
    public ApiResponse<ProjectTaskResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectTaskRequest request
    ) {
        return ApiResponse.success(taskService.create(projectId, request));
    }

    /** 完整修改指定项目的领取任务配置 */
    @PutMapping
    public ApiResponse<ProjectTaskResponse> update(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectTaskRequest request
    ) {
        return ApiResponse.success(taskService.update(projectId, request));
    }

    /** 根据项目和任务配置生成60个公司名称候选 */
    @PostMapping("/company-names/generate")
    public ApiResponse<CompanyNamesResponse> generateCompanyNames(
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(taskService.generateCompanyNames(projectId));
    }

    /** 修改指定下标的公司名称候选。 */
    @PutMapping("/company-names/{index}")
    public ApiResponse<CompanyNamesResponse> updateCompanyName(
            @PathVariable Long projectId,
            @PathVariable int index,
            @Valid @RequestBody UpdateCompanyNameRequest request
    ) {
        return ApiResponse.success(taskService.updateCompanyName(projectId, index, request));
    }

    /**
     * 上传任务图片
     */
    @PostMapping("/images/upload")
    public ApiResponse<TaskImageUploadResponse> uploadImages(
            @PathVariable Long projectId,
            @RequestParam(value = "taskImage", required = false) MultipartFile taskImage,
            @RequestParam(value = "submissionIcon", required = false) MultipartFile submissionIcon,
            @RequestParam(value = "companyCard", required = false) MultipartFile companyCard
    ) {
        return ApiResponse.success(
                taskService.uploadImages(projectId, taskImage, submissionIcon, companyCard)
        );
    }
}
