package com.ai4.generation.repository;
import com.ai4.generation.model.ProjectGeneration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;
/** 使用 JdbcTemplate 操作项目生成配置。 */
@Repository
public class JdbcProjectGenerationRepository implements ProjectGenerationRepository {
    private final JdbcTemplate jdbcTemplate;
    public JdbcProjectGenerationRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    public Optional<ProjectGeneration> findByProjectId(Long projectId) {
        return jdbcTemplate.query("SELECT id, project_id, scoring_criteria, created_at, updated_at FROM ai4_project_generation WHERE project_id = ?",
                (rs, rowNum) -> new ProjectGeneration(rs.getLong("id"), rs.getLong("project_id"),
                        rs.getString("scoring_criteria"), rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()), projectId).stream().findFirst();
    }
    public ProjectGeneration save(Long projectId, String scoringCriteria) {
        jdbcTemplate.update("INSERT INTO ai4_project_generation(project_id, scoring_criteria) VALUES (?, ?)", projectId, scoringCriteria);
        return findByProjectId(projectId).orElseThrow();
    }
    public int update(Long projectId, String scoringCriteria) {
        return jdbcTemplate.update("UPDATE ai4_project_generation SET scoring_criteria = ? WHERE project_id = ?", scoringCriteria, projectId);
    }
}
