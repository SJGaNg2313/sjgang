package com.ai4.report.service;
import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.project.repository.ProjectRepository;
import com.ai4.report.dto.SaveProjectReportRequest;
import com.ai4.report.model.ProjectReport;
import com.ai4.report.repository.ProjectReportRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** 创业报告配置业务。 */
@Service
public class ProjectReportService {
    private final ProjectReportRepository repository;
    private final ProjectRepository projectRepository;
    public ProjectReportService(ProjectReportRepository repository, ProjectRepository projectRepository) {
        this.repository = repository; this.projectRepository = projectRepository;
    }
    @Transactional(readOnly = true)
    public ProjectReport get(Long projectId) {
        requireProject(projectId);
        return repository.findByProjectId(projectId).orElseThrow(() -> new BizException(ErrorCode.PROJECT_REPORT_NOT_FOUND));
    }
    @Transactional
    public ProjectReport create(Long projectId, SaveProjectReportRequest request) {
        requireProject(projectId);
        if (repository.findByProjectId(projectId).isPresent()) throw new BizException(ErrorCode.PROJECT_REPORT_ALREADY_EXISTS);
        try { return repository.save(projectId, request.reviewCriteria().trim()); }
        catch (DuplicateKeyException e) { throw new BizException(ErrorCode.PROJECT_REPORT_ALREADY_EXISTS); }
    }
    @Transactional
    public ProjectReport update(Long projectId, SaveProjectReportRequest request) {
        requireProject(projectId);
        if (repository.update(projectId, request.reviewCriteria().trim()) == 0) throw new BizException(ErrorCode.PROJECT_REPORT_NOT_FOUND);
        return repository.findByProjectId(projectId).orElseThrow();
    }
    private void requireProject(Long projectId) {
        projectRepository.findById(projectId).orElseThrow(() -> new BizException(ErrorCode.PROJECT_NOT_FOUND));
    }
}
