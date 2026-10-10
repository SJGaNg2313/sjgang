package com.ai4.project.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.course.model.CoursePackage;
import com.ai4.course.repository.CoursePackageRepository;
import com.ai4.project.dto.CreateProjectRequest;
import com.ai4.project.dto.ProjectDetailResponse;
import com.ai4.project.dto.ProjectListItemResponse;
import com.ai4.project.dto.ProjectResponse;
import com.ai4.project.dto.ReorderProjectsRequest;
import com.ai4.project.dto.UpdateProjectRequest;
import com.ai4.project.model.Project;
import com.ai4.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 项目的列表、详情、创建、修改和逻辑删除 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CoursePackageRepository packageRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            CoursePackageRepository packageRepository
    ) {
        this.projectRepository = projectRepository;
        this.packageRepository = packageRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectListItemResponse> list() {
        return projectRepository.findAllWithConfigurationStatus()
                .stream()
                .map(project -> new ProjectListItemResponse(
                        project.id(),
                        project.projectName(),
                        project.sort(),
                        project.configurationStatus()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse get(Long id) {
        Project project = findProject(id);
        return new ProjectDetailResponse(
                project.projectName(),
                project.projectIntro(),
                project.projectCode(),
                project.sort()
        );
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        CoursePackage coursePackage = findOnlyPackage();
        int sort = projectRepository.findNextSort(coursePackage.id());
        Project project = projectRepository.save(
                coursePackage.id(),
                request.projectName().trim(),
                request.projectIntro().trim(),
                request.projectCode().trim(),
                sort
        );
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request) {
        findProject(id);
        if (projectRepository.update(
                id,
                request.projectName().trim(),
                request.projectIntro().trim(),
                request.projectCode().trim()
        ) == 0) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return toResponse(findProject(id));
    }

    @Transactional
    public void reorder(ReorderProjectsRequest request) {
        CoursePackage coursePackage = findOnlyPackage();
        List<Project> activeProjects = projectRepository.findByPackageId(coursePackage.id());
        List<Long> projectIds = request.projectIds();
        Set<Long> requestedIds = new HashSet<>(projectIds);
        Set<Long> activeIds = activeProjects.stream()
                .map(Project::id)
                .collect(java.util.stream.Collectors.toSet());
        if (requestedIds.size() != projectIds.size()
                || requestedIds.size() != activeIds.size()
                || !requestedIds.equals(activeIds)) {
            throw new BizException(ErrorCode.PROJECT_ORDER_INVALID);
        }

        for (int index = 0; index < projectIds.size(); index++) {
            if (projectRepository.updateSort(
                    coursePackage.id(),
                    projectIds.get(index),
                    index + 1
            ) == 0) {
                throw new BizException(ErrorCode.PROJECT_ORDER_INVALID);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        findProject(id);
        if (projectRepository.softDelete(id) == 0) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private CoursePackage findOnlyPackage() {
        return packageRepository.findOnlyActive()
                .orElseThrow(() -> new BizException(ErrorCode.COURSE_PACKAGE_CONFIGURATION_INVALID));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.id(),
                project.packageId(),
                project.projectName(),
                project.projectIntro(),
                project.projectCode(),
                project.sort(),
                project.createdAt(),
                project.updatedAt()
        );
    }
}
