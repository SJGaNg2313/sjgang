package com.ai4.generation.repository;
import com.ai4.generation.model.ProjectGeneration;
import java.util.Optional;
/** 项目生成配置数据访问边界。 */
public interface ProjectGenerationRepository {
    Optional<ProjectGeneration> findByProjectId(Long projectId);
    ProjectGeneration save(Long projectId, String scoringCriteria);
    int update(Long projectId, String scoringCriteria);
}
