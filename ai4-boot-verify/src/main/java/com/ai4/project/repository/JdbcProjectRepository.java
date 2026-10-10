package com.ai4.project.repository;

import com.ai4.project.model.Project;
import com.ai4.project.model.ProjectListItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** 使用 JdbcTemplate 操作项目表 */
@Repository
public class JdbcProjectRepository implements ProjectRepository {

    private static final String COLUMNS = """
            id, package_id, project_name, project_intro, project_code, `sort` AS project_sort,
            del_flag, created_at, updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ProjectListItem> findAllWithConfigurationStatus() {
        return jdbcTemplate.query(
                """
                SELECT
                    project.id,
                    project.project_name,
                    project.`sort` AS project_sort,
                    CASE
                        WHEN task.id IS NOT NULL
                         AND project_generation.id IS NOT NULL
                         AND sandtable.id IS NOT NULL
                         AND report.id IS NOT NULL
                        THEN 'COMPLETED'
                        ELSE 'INCOMPLETE'
                    END AS configuration_status
                FROM ai4_project project
                LEFT JOIN ai4_project_task task
                    ON task.project_id = project.id
                LEFT JOIN ai4_project_generation project_generation
                    ON project_generation.project_id = project.id
                LEFT JOIN ai4_project_sandtable sandtable
                    ON sandtable.project_id = project.id
                LEFT JOIN ai4_project_report report
                    ON report.project_id = project.id
                WHERE project.del_flag = 1
                ORDER BY project.`sort` ASC, project.id ASC
                """,
                (resultSet, rowNumber) -> new ProjectListItem(
                        resultSet.getLong("id"),
                        resultSet.getString("project_name"),
                        resultSet.getObject("project_sort", Integer.class),
                        resultSet.getString("configuration_status")
                )
        );
    }

    @Override
    public Optional<Project> findById(Long id) {
        List<Project> projects = jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM ai4_project WHERE id = ? AND del_flag = 1",
                rowMapper(),
                id
        );
        return projects.stream().findFirst();
    }

    @Override
    public List<Project> findByPackageId(Long packageId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM ai4_project "
                        + "WHERE package_id = ? AND del_flag = 1 "
                        + "ORDER BY `sort` ASC, id ASC",
                rowMapper(),
                packageId
        );
    }

    @Override
    public int findNextSort(Long packageId) {
        Integer nextSort = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(`sort`), 0) + 1 FROM ai4_project "
                        + "WHERE package_id = ? AND del_flag = 1",
                Integer.class,
                packageId
        );
        return nextSort == null ? 1 : nextSort;
    }

    @Override
    public Project save(
            Long packageId,
            String projectName,
            String projectIntro,
            String projectCode,
            int sort
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO ai4_project("
                            + "package_id, project_name, project_intro, project_code, `sort`, del_flag"
                            + ") VALUES (?, ?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, packageId);
            statement.setString(2, projectName);
            statement.setString(3, projectIntro);
            statement.setString(4, projectCode);
            statement.setInt(5, sort);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to obtain generated project ID");
        }
        return findById(key.longValue())
                .orElseThrow(() -> new IllegalStateException("Failed to query saved project"));
    }

    @Override
    public int update(Long id, String projectName, String projectIntro, String projectCode) {
        return jdbcTemplate.update(
                "UPDATE ai4_project "
                        + "SET project_name = ?, project_intro = ?, project_code = ? "
                        + "WHERE id = ? AND del_flag = 1",
                projectName,
                projectIntro,
                projectCode,
                id
        );
    }

    @Override
    public int updateSort(Long packageId, Long projectId, int sort) {
        return jdbcTemplate.update(
                "UPDATE ai4_project SET `sort` = ? "
                        + "WHERE id = ? AND package_id = ? AND del_flag = 1",
                sort,
                projectId,
                packageId
        );
    }

    @Override
    public int softDelete(Long id) {
        return jdbcTemplate.update(
                "UPDATE ai4_project SET del_flag = 2 WHERE id = ? AND del_flag = 1",
                id
        );
    }

    private RowMapper<Project> rowMapper() {
        return (resultSet, rowNumber) -> new Project(
                resultSet.getLong("id"),
                resultSet.getLong("package_id"),
                resultSet.getString("project_name"),
                resultSet.getString("project_intro"),
                resultSet.getString("project_code"),
                resultSet.getObject("project_sort", Integer.class),
                resultSet.getInt("del_flag"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }

}
