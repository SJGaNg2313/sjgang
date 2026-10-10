package com.ai4.generation.controller;
import com.ai4.common.response.ApiResponse;
import com.ai4.generation.dto.SaveProjectGenerationRequest;
import com.ai4.generation.model.ProjectGeneration;
import com.ai4.generation.service.ProjectGenerationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
/** 项目生成配置接口。 */
@RestController
@RequestMapping("/projects/{projectId}/generation")
public class ProjectGenerationController {
    private final ProjectGenerationService service;
    public ProjectGenerationController(ProjectGenerationService service) { this.service = service; }
    @GetMapping
    public ApiResponse<ProjectGeneration> get(@PathVariable Long projectId) { return ApiResponse.success(service.get(projectId)); }
    @PostMapping
    public ApiResponse<ProjectGeneration> create(@PathVariable Long projectId, @Valid @RequestBody SaveProjectGenerationRequest request) {
        return ApiResponse.success(service.create(projectId, request));
    }
    @PutMapping
    public ApiResponse<ProjectGeneration> update(@PathVariable Long projectId, @Valid @RequestBody SaveProjectGenerationRequest request) {
        return ApiResponse.success(service.update(projectId, request));
    }
}
