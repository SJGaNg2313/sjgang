package com.ai4.task.repository;

import com.ai4.task.model.ProjectTask;
import com.ai4.task.model.ProjectTaskPartnerSkills;

import java.util.List;
import java.util.Optional;

/** 项目领取任务数据访问边界 */
public interface ProjectTaskRepository {

    Optional<ProjectTask> findByProjectId(Long projectId);

    List<ProjectTaskPartnerSkills> findPartnerSkills(Long taskId);

    List<String> lockActivePartnerIds(List<String> partnerIds);

    List<String> lockActiveSkillIds(List<String> skillIds);

    ProjectTask save(ProjectTask task);

    int update(ProjectTask task);

    int updateCompanyNames(Long projectId, String companyNames);

    void replacePartnerRelations(Long taskId, List<ProjectTaskPartnerSkills> partnerSkills);
}
