package com.ai4.sandtable.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.sandtable.dto.ProjectSandtableResponse;
import com.ai4.sandtable.dto.SandtableVideoUploadResponse;
import com.ai4.sandtable.dto.SandtableTrendUploadResponse;
import com.ai4.sandtable.dto.SaveProjectSandtableRequest;
import com.ai4.sandtable.service.ProjectSandtableService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 项目沙盘配置接口。 */
@RestController
@RequestMapping("/projects/{projectId}/sandtable")
public class ProjectSandtableController {

    private final ProjectSandtableService sandtableService;

    public ProjectSandtableController(ProjectSandtableService sandtableService) {
        this.sandtableService = sandtableService;
    }

    /** 查询项目沙盘完整配置。 */
    @GetMapping
    public ApiResponse<ProjectSandtableResponse> get(@PathVariable Long projectId) {
        return ApiResponse.success(sandtableService.get(projectId));
    }

    /** 首次创建项目沙盘配置。 */
    @PostMapping
    public ApiResponse<ProjectSandtableResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody SaveProjectSandtableRequest request
    ) {
        return ApiResponse.success(sandtableService.create(projectId, request));
    }

    /** 完整修改项目沙盘配置。 */
    @PutMapping
    public ApiResponse<ProjectSandtableResponse> update(
            @PathVariable Long projectId,
            @Valid @RequestBody SaveProjectSandtableRequest request
    ) {
        return ApiResponse.success(sandtableService.update(projectId, request));
    }

    /** 上传项目沙盘视频。 */
    @PostMapping("/video/upload")
    public ApiResponse<SandtableVideoUploadResponse> uploadVideo(
            @PathVariable Long projectId,
            @RequestParam("video") MultipartFile video
    ) {
        return ApiResponse.success(sandtableService.uploadVideo(projectId, video));
    }

    /** 上传项目沙盘收益趋势图。 */
    @PostMapping("/trend/upload")
    public ApiResponse<SandtableTrendUploadResponse> uploadTrend(
            @PathVariable Long projectId,
            @RequestParam("trend") MultipartFile trend
    ) {
        return ApiResponse.success(sandtableService.uploadTrend(projectId, trend));
    }
}
