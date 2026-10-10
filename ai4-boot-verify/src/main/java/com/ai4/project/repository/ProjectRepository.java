package com.ai4.project.repository;

import com.ai4.project.model.Project;
import com.ai4.project.model.ProjectListItem;

import java.util.List;
import java.util.Optional;

/** 项目数据访问边界 */
public interface ProjectRepository {

    List<ProjectListItem> findAllWithConfigurationStatus();

    Optional<Project> findById(Long id);

    List<Project> findByPackageId(Long packageId);

    int findNextSort(Long packageId);

    Project save(
            Long packageId,
            String projectName,
            String projectIntro,
            String projectCode,
            int sort
    );

    int update(Long id, String projectName, String projectIntro, String projectCode);

    int updateSort(Long packageId, Long projectId, int sort);

    int softDelete(Long id);
}
