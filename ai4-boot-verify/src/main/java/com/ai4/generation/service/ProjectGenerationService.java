package com.ai4.generation.service;
import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.generation.dto.SaveProjectGenerationRequest;
import com.ai4.generation.model.ProjectGeneration;
import com.ai4.generation.repository.ProjectGenerationRepository;
import com.ai4.project.repository.ProjectRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** 项目生成配置业务。 */
@Service
public class ProjectGenerationService {
    private final ProjectGenerationRepository repository;
    private final ProjectRepository projectRepository;
    public ProjectGenerationService(ProjectGenerationRepository repository, ProjectRepository projectRepository) {
        this.repository = repository; this.projectRepository = projectRepository;
    }
    @Transactional(readOnly = true)
    public ProjectGeneration get(Long projectId) {
        requireProject(projectId);
        return repository.findByProjectId(projectId).orElseThrow(() -> new BizException(ErrorCode.PROJECT_GENERATION_NOT_FOUND));
    }
    @Transactional
    public ProjectGeneration create(Long projectId, SaveProjectGenerationRequest request) {
        requireProject(projectId);
        if (repository.findByProjectId(projectId).isPresent()) throw new BizException(ErrorCode.PROJECT_GENERATION_ALREADY_EXISTS);
        try { return repository.save(projectId, request.scoringCriteria().trim()); }
        catch (DuplicateKeyException e) { throw new BizException(ErrorCode.PROJECT_GENERATION_ALREADY_EXISTS); }
    }
    @Transactional
    public ProjectGeneration update(Long projectId, SaveProjectGenerationRequest request) {
        requireProject(projectId);
        if (repository.update(projectId, request.scoringCriteria().trim()) == 0) throw new BizException(ErrorCode.PROJECT_GENERATION_NOT_FOUND);
        return repository.findByProjectId(projectId).orElseThrow();
    }
    private void requireProject(Long projectId) {
        projectRepository.findById(projectId).orElseThrow(() -> new BizException(ErrorCode.PROJECT_NOT_FOUND));
    }
}
