package com.ai4.skill.repository;

import com.ai4.skill.dto.SkillListItemResponse;
import com.ai4.skill.dto.SkillOptionResponse;
import com.ai4.skill.model.Skill;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 使用 JdbcTemplate 操作 Skill 表 */
@Repository
public class JdbcSkillRepository implements SkillRepository {

    private static final String DETAIL_COLUMNS = """
            id, skill_name, skill_content, skill_icon, trigger_conditions,
            input_parameters, follow_step, output_format, constraints,
            del_flag, created_at, updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Skill> skillRowMapper = (resultSet, rowNum) -> new Skill(
            resultSet.getString("id"),
            resultSet.getString("skill_name"),
            resultSet.getString("skill_content"),
            resultSet.getString("skill_icon"),
            resultSet.getString("trigger_conditions"),
            resultSet.getString("input_parameters"),
            resultSet.getString("follow_step"),
            resultSet.getString("output_format"),
            resultSet.getString("constraints"),
            resultSet.getInt("del_flag"),
            resultSet.getTimestamp("created_at").toLocalDateTime(),
            resultSet.getTimestamp("updated_at").toLocalDateTime()
    );

    public JdbcSkillRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai4_skill WHERE del_flag = 1",
                Long.class
        );
        return count == null ? 0 : count;
    }

    @Override
    public List<SkillListItemResponse> findPage(int offset, int limit) {
        return jdbcTemplate.query(
                "SELECT skill.id, skill.skill_name, skill.skill_content, skill.skill_icon, "
                        + "(SELECT COUNT(DISTINCT relation.partner_id) "
                        + "FROM ai4_project_task_partner_skill relation "
                        + "JOIN ai4_partner partner ON partner.id = relation.partner_id "
                        + "AND partner.del_flag = 1 "
                        + "WHERE relation.skill_id = skill.id) AS partner_count "
                        + "FROM ai4_skill skill WHERE skill.del_flag = 1 "
                        + "ORDER BY skill.created_at DESC, skill.id DESC LIMIT ? OFFSET ?",
                (resultSet, rowNum) -> new SkillListItemResponse(
                        resultSet.getString("id"),
                        resultSet.getString("skill_name"),
                        resultSet.getString("skill_content"),
                        resultSet.getString("skill_icon"),
                        resultSet.getLong("partner_count")
                ),
                limit,
                offset
        );
    }

    @Override
    public List<SkillOptionResponse> findOptions() {
        return jdbcTemplate.query(
                "SELECT id, skill_name, skill_icon FROM ai4_skill "
                        + "WHERE del_flag = 1 ORDER BY skill_name, id",
                (resultSet, rowNum) -> new SkillOptionResponse(
                        resultSet.getString("id"),
                        resultSet.getString("skill_name"),
                        resultSet.getString("skill_icon")
                )
        );
    }

    @Override
    public boolean existsByName(String skillName, String excludeSkillId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai4_skill WHERE del_flag = 1 "
                        + "AND skill_name = ? AND (? IS NULL OR id <> ?)",
                Integer.class,
                skillName,
                excludeSkillId,
                excludeSkillId
        );
        return count != null && count > 0;
    }

    @Override
    public Optional<Skill> findById(String skillId) {
        List<Skill> skills = jdbcTemplate.query(
                "SELECT " + DETAIL_COLUMNS
                        + " FROM ai4_skill WHERE id = ? AND del_flag = 1",
                skillRowMapper,
                skillId
        );
        return skills.stream().findFirst();
    }

    @Override
    public boolean hasTaskRelations(String skillId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai4_project_task_partner_skill WHERE skill_id = ?",
                Integer.class,
                skillId
        );
        return count != null && count > 0;
    }

    @Override
    public int save(Skill skill) {
        return jdbcTemplate.update(
                "INSERT INTO ai4_skill("
                        + "id, skill_name, skill_content, skill_icon, trigger_conditions, "
                        + "input_parameters, follow_step, output_format, constraints, del_flag"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                skill.id(),
                skill.skillName(),
                skill.skillContent(),
                skill.skillIcon(),
                skill.triggerConditions(),
                skill.inputParameters(),
                skill.followStep(),
                skill.outputFormat(),
                skill.constraints()
        );
    }

    @Override
    public int update(Skill skill) {
        return jdbcTemplate.update(
                "UPDATE ai4_skill SET skill_name = ?, skill_content = ?, skill_icon = ?, "
                        + "trigger_conditions = ?, input_parameters = ?, follow_step = ?, "
                        + "output_format = ?, constraints = ? "
                        + "WHERE id = ? AND del_flag = 1",
                skill.skillName(),
                skill.skillContent(),
                skill.skillIcon(),
                skill.triggerConditions(),
                skill.inputParameters(),
                skill.followStep(),
                skill.outputFormat(),
                skill.constraints(),
                skill.id()
        );
    }

    @Override
    public int softDelete(String skillId) {
        return jdbcTemplate.update(
                "UPDATE ai4_skill SET del_flag = 2 "
                        + "WHERE id = ? AND del_flag = 1 "
                        + "AND NOT EXISTS ("
                        + "SELECT 1 FROM ai4_project_task_partner_skill relation "
                        + "WHERE relation.skill_id = ai4_skill.id"
                        + ")",
                skillId
        );
    }
}
