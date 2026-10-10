package com.ai4.report.repository;
import com.ai4.report.model.ProjectReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;
/** 使用 JdbcTemplate 操作创业报告配置。 */
@Repository
public class JdbcProjectReportRepository implements ProjectReportRepository {
    private final JdbcTemplate jdbcTemplate;
    public JdbcProjectReportRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    public Optional<ProjectReport> findByProjectId(Long projectId) {
        return jdbcTemplate.query("SELECT id, project_id, review_criteria, created_at, updated_at FROM ai4_project_report WHERE project_id = ?",
                (rs, rowNum) -> new ProjectReport(rs.getLong("id"), rs.getLong("project_id"),
                        rs.getString("review_criteria"), rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()), projectId).stream().findFirst();
    }
    public ProjectReport save(Long projectId, String reviewCriteria) {
        jdbcTemplate.update("INSERT INTO ai4_project_report(project_id, review_criteria) VALUES (?, ?)", projectId, reviewCriteria);
        return findByProjectId(projectId).orElseThrow();
    }
    public int update(Long projectId, String reviewCriteria) {
        return jdbcTemplate.update("UPDATE ai4_project_report SET review_criteria = ? WHERE project_id = ?", reviewCriteria, projectId);
    }
}
