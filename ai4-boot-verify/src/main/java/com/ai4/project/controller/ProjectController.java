package com.ai4.project.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.project.dto.CreateProjectRequest;
import com.ai4.project.dto.ProjectDetailResponse;
import com.ai4.project.dto.ProjectListItemResponse;
import com.ai4.project.dto.ProjectResponse;
import com.ai4.project.dto.ReorderProjectsRequest;
import com.ai4.project.dto.UpdateProjectRequest;
import com.ai4.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 项目管理接口。 */
@RestController
@RequestMapping("/projects")
public class

 ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** 查询全部有效项目，并返回四个业务模块的整体配置状态 */
    @GetMapping
    public ApiResponse<List<ProjectListItemResponse>> list() {
        return ApiResponse.success(projectService.list());
    }

    /** 查询项目详情 */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> get(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.get(projectId));
    }

    /** 新增项目并自动绑定系统中唯一的有效课包 */
    @PostMapping
    public ApiResponse<ProjectResponse> create(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        return ApiResponse.success(projectService.create(request));
    }

    /** 修改项目名称、介绍和唯一编码 */
    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ApiResponse.success(projectService.update(projectId, request));
    }

    /** 调整当前课包下项目顺序，供后续新建班级排课使用。 */
    @PutMapping("/order")
    public ApiResponse<Void> reorder(
            @Valid @RequestBody ReorderProjectsRequest request
    ) {
        projectService.reorder(request);
        return ApiResponse.success(null);
    }

    /** 逻辑删除项目。 */
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId) {
        projectService.delete(projectId);
        return ApiResponse.success(null);
    }
}
