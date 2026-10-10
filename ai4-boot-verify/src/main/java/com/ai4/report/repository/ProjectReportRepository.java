package com.ai4.report.repository;
import com.ai4.report.model.ProjectReport;
import java.util.Optional;
/** 创业报告配置数据访问边界。 */
public interface ProjectReportRepository {
    Optional<ProjectReport> findByProjectId(Long projectId);
    ProjectReport save(Long projectId, String reviewCriteria);
    int update(Long projectId, String reviewCriteria);
}
