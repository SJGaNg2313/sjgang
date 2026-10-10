package com.ai4.report.controller;
import com.ai4.common.response.ApiResponse;
import com.ai4.report.dto.SaveProjectReportRequest;
import com.ai4.report.model.ProjectReport;
import com.ai4.report.service.ProjectReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
/** 创业报告配置接口。 */
@RestController
@RequestMapping("/projects/{projectId}/report")
public class ProjectReportController {
    private final ProjectReportService service;
    public ProjectReportController(ProjectReportService service) { this.service = service; }
    @GetMapping
    public ApiResponse<ProjectReport> get(@PathVariable Long projectId) { return ApiResponse.success(service.get(projectId)); }
    @PostMapping
    public ApiResponse<ProjectReport> create(@PathVariable Long projectId, @Valid @RequestBody SaveProjectReportRequest request) {
        return ApiResponse.success(service.create(projectId, request));
    }
    @PutMapping
    public ApiResponse<ProjectReport> update(@PathVariable Long projectId, @Valid @RequestBody SaveProjectReportRequest request) {
        return ApiResponse.success(service.update(projectId, request));
    }
}
