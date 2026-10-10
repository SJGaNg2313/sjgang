package com.ai4.task.repository;

import com.ai4.task.model.ProjectTask;
import com.ai4.task.model.ProjectTaskPartnerSkills;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/** 使用 JdbcTemplate 操作项目领取任务及合伙人关联 */
@Repository
public class JdbcProjectTaskRepository implements ProjectTaskRepository {

    private static final String COLUMNS = """
            id, project_id, task_types, company_positioning, company_card,
            company_names, task_name, task_image, task_objective, task_keywords,
            submission_icon, submission_name, submission_description,
            created_at, updated_at
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<ProjectTask> rowMapper = (resultSet, rowNum) -> new ProjectTask(
            resultSet.getLong("id"),
            resultSet.getLong("project_id"),
            resultSet.getString("task_types"),
            resultSet.getString("company_positioning"),
            resultSet.getString("company_card"),
            resultSet.getString("company_names"),
            resultSet.getString("task_name"),
            resultSet.getString("task_image"),
            resultSet.getString("task_objective"),
            resultSet.getString("task_keywords"),
            resultSet.getString("submission_icon"),
            resultSet.getString("submission_name"),
            resultSet.getString("submission_description"),
            resultSet.getTimestamp("created_at").toLocalDateTime(),
            resultSet.getTimestamp("updated_at").toLocalDateTime()
    );

    public JdbcProjectTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ProjectTask> findByProjectId(Long projectId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM ai4_project_task WHERE project_id = ?",
                rowMapper,
                projectId
        ).stream().findFirst();
    }

    @Override
    public List<ProjectTaskPartnerSkills> findPartnerSkills(Long taskId) {
        Map<String, List<String>> skillIdsByPartner = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT partner.partner_id, skill.skill_id"
                        + " FROM ai4_project_task_partner partner"
                        + " LEFT JOIN ai4_project_task_partner_skill skill"
                        + " ON skill.task_id = partner.task_id"
                        + " AND skill.partner_id = partner.partner_id"
                        + " WHERE partner.task_id = ?"
                        + " ORDER BY partner.sort_order, partner.partner_id,"
                        + " skill.sort_order, skill.skill_id",
                resultSet -> {
                    String partnerId = resultSet.getString("partner_id");
                    List<String> skillIds = skillIdsByPartner
                            .computeIfAbsent(partnerId, ignored -> new ArrayList<>());
                    String skillId = resultSet.getString("skill_id");
                    if (skillId != null) {
                        skillIds.add(skillId);
                    }
                },
                taskId
        );
        return skillIdsByPartner.entrySet().stream()
                .map(entry -> new ProjectTaskPartnerSkills(
                        entry.getKey(), List.copyOf(entry.getValue())
                ))
                .toList();
    }

    @Override
    public List<String> lockActivePartnerIds(List<String> partnerIds) {
        if (partnerIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT id FROM ai4_partner WHERE del_flag = 1 AND id IN ("
                        + placeholders(partnerIds.size()) + ") ORDER BY id FOR UPDATE",
                (resultSet, rowNum) -> resultSet.getString("id"),
                partnerIds.toArray()
        );
    }

    @Override
    public List<String> lockActiveSkillIds(List<String> skillIds) {
        if (skillIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT id FROM ai4_skill WHERE del_flag = 1 AND id IN ("
                        + placeholders(skillIds.size()) + ") ORDER BY id FOR UPDATE",
                (resultSet, rowNum) -> resultSet.getString("id"),
                skillIds.toArray()
        );
    }

    @Override
    public ProjectTask save(ProjectTask task) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO ai4_project_task("
                            + "project_id, task_types, company_positioning, company_card,"
                            + " task_name, task_image, task_objective, task_keywords,"
                            + " submission_icon, submission_name, submission_description"
                            + ") VALUES (?, CAST(? AS JSON), ?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, task.projectId());
            statement.setString(2, task.taskTypes());
            statement.setString(3, task.companyPositioning());
            statement.setString(4, task.companyCard());
            statement.setString(5, task.taskName());
            statement.setString(6, task.taskImage());
            statement.setString(7, task.taskObjective());
            statement.setString(8, task.taskKeywords());
            statement.setString(9, task.submissionIcon());
            statement.setString(10, task.submissionName());
            statement.setString(11, task.submissionDescription());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to obtain generated task ID");
        }
        return findByProjectId(task.projectId())
                .orElseThrow(() -> new IllegalStateException("Failed to query saved task"));
    }

    @Override
    public int update(ProjectTask task) {
        return jdbcTemplate.update(
                "UPDATE ai4_project_task SET task_types = CAST(? AS JSON),"
                        + " company_positioning = ?, company_card = ?,"
                        + " task_name = ?, task_image = ?, task_objective = ?,"
                        + " task_keywords = CAST(? AS JSON), submission_icon = ?,"
                        + " submission_name = ?, submission_description = ?"
                        + " WHERE project_id = ?",
                task.taskTypes(), task.companyPositioning(), task.companyCard(),
                task.taskName(), task.taskImage(), task.taskObjective(),
                task.taskKeywords(), task.submissionIcon(), task.submissionName(),
                task.submissionDescription(), task.projectId()
        );
    }

    @Override
    public int updateCompanyNames(Long projectId, String companyNames) {
        return jdbcTemplate.update(
                "UPDATE ai4_project_task SET company_names = CAST(? AS JSON)"
                        + " WHERE project_id = ?",
                companyNames,
                projectId
        );
    }

    @Override
    public void replacePartnerRelations(
            Long taskId,
            List<ProjectTaskPartnerSkills> partnerSkills
    ) {
        jdbcTemplate.update(
                "DELETE FROM ai4_project_task_partner_skill WHERE task_id = ?",
                taskId
        );
        jdbcTemplate.update(
                "DELETE FROM ai4_project_task_partner WHERE task_id = ?",
                taskId
        );
        List<Integer> indexes = IntStream.range(0, partnerSkills.size()).boxed().toList();
        jdbcTemplate.batchUpdate(
                "INSERT INTO ai4_project_task_partner(task_id, partner_id, sort_order)"
                        + " VALUES (?, ?, ?)",
                indexes,
                indexes.size(),
                (statement, index) -> {
                    statement.setLong(1, taskId);
                    statement.setString(2, partnerSkills.get(index).partnerId());
                    statement.setInt(3, index);
                }
        );

        List<PartnerSkillBinding> skillBindings = new ArrayList<>();
        for (ProjectTaskPartnerSkills partner : partnerSkills) {
            for (int index = 0; index < partner.skillIds().size(); index++) {
                skillBindings.add(new PartnerSkillBinding(
                        partner.partnerId(), partner.skillIds().get(index), index
                ));
            }
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO ai4_project_task_partner_skill"
                        + "(task_id, partner_id, skill_id, sort_order) VALUES (?, ?, ?, ?)",
                skillBindings,
                skillBindings.size(),
                (statement, binding) -> {
                    statement.setLong(1, taskId);
                    statement.setString(2, binding.partnerId());
                    statement.setString(3, binding.skillId());
                    statement.setInt(4, binding.sortOrder());
                }
        );
    }

    private record PartnerSkillBinding(
            String partnerId,
            String skillId,
            int sortOrder
    ) {
    }

    private String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }
}
