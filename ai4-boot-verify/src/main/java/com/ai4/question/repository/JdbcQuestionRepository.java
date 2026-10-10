package com.ai4.question.repository;

import com.ai4.question.dto.CreateQuestionRequest;
import com.ai4.question.dto.ProjectQuestionListItemResponse;
import com.ai4.question.dto.QuestionAttributeResponse;
import com.ai4.question.dto.QuestionOptionAttributeResponse;
import com.ai4.question.dto.QuestionOptionRequest;
import com.ai4.question.dto.QuestionOptionResponse;
import com.ai4.question.dto.QuestionPageQuery;
import com.ai4.question.dto.QuestionProjectAttributeResponse;
import com.ai4.question.dto.UpdateQuestionRequest;
import com.ai4.question.model.ProjectQuestion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 使用 JdbcTemplate 查询和保存项目题目管理数据。 */
@Repository
public class JdbcQuestionRepository implements QuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ProjectQuestion> questionRowMapper = (resultSet, rowNumber) ->
            new ProjectQuestion(
                    resultSet.getLong("id"),
                    resultSet.getLong("project_id"),
                    resultSet.getString("image_url"),
                    resultSet.getString("image_desc"),
                    resultSet.getString("question_text"),
                    resultSet.getInt("month"),
                    resultSet.getString("before_guide"),
                    resultSet.getInt("status"),
                    resultSet.getString("remark"),
                    resultSet.getInt("del_flag"),
                    resultSet.getTimestamp("created_at").toLocalDateTime(),
                    resultSet.getTimestamp("updated_at").toLocalDateTime()
            );

    public JdbcQuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<QuestionProjectAttributeResponse> findProjectAttributes() {
        Map<Long, ProjectAttributeBuilder> projects = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT project.id AS project_id, project.project_name, "
                        + "attribute.id AS attribute_id, attribute.attribute_name "
                        + "FROM ai4_project project "
                        + "LEFT JOIN ai4_project_sandtable sandtable "
                        + "ON sandtable.project_id = project.id "
                        + "LEFT JOIN ai4_project_sandtable_attribute attribute "
                        + "ON attribute.sandtable_id = sandtable.id "
                        + "WHERE project.del_flag = 1 "
                        + "ORDER BY project.created_at ASC, project.id ASC, attribute.id ASC",
                resultSet -> {
                    Long projectId = resultSet.getLong("project_id");
                    ProjectAttributeBuilder builder = projects.computeIfAbsent(
                            projectId,
                            id -> new ProjectAttributeBuilder(
                                    id,
                                    getString(resultSet, "project_name"),
                                    new ArrayList<>()
                            )
                    );
                    Long attributeId = getNullableLong(resultSet, "attribute_id");
                    if (attributeId != null) {
                        builder.attributes().add(new QuestionAttributeResponse(
                                attributeId,
                                getString(resultSet, "attribute_name")
                        ));
                    }
                }
        );
        return projects.values()
                .stream()
                .map(builder -> new QuestionProjectAttributeResponse(
                        builder.projectId(),
                        builder.projectName(),
                        builder.attributes()
                ))
                .toList();
    }

    @Override
    public List<QuestionAttributeResponse> findAttributesByProjectId(Long projectId) {
        return jdbcTemplate.query(
                "SELECT attribute.id AS attribute_id, attribute.attribute_name "
                        + "FROM ai4_project_sandtable sandtable "
                        + "JOIN ai4_project_sandtable_attribute attribute "
                        + "ON attribute.sandtable_id = sandtable.id "
                        + "WHERE sandtable.project_id = ? "
                        + "ORDER BY attribute.id ASC",
                (resultSet, rowNumber) -> new QuestionAttributeResponse(
                        resultSet.getLong("attribute_id"),
                        resultSet.getString("attribute_name")
                ),
                projectId
        );
    }

    @Override
    public long countPage(QuestionPageQuery query) {
        List<Object> parameters = new ArrayList<>();
        String sql = buildPageWhereSql(query, parameters, "SELECT COUNT(*) FROM ai4_project_question question");
        Long count = jdbcTemplate.queryForObject(sql, Long.class, parameters.toArray());
        return count == null ? 0 : count;
    }

    @Override
    public List<ProjectQuestionListItemResponse> findPage(QuestionPageQuery query) {
        List<Object> parameters = new ArrayList<>();
        String sql = buildPageWhereSql(
                query,
                parameters,
                "SELECT question.id, question.month, question.question_text, question.status, question.remark "
                        + "FROM ai4_project_question question"
        ) + " ORDER BY question.month ASC, question.id ASC LIMIT ? OFFSET ?";
        parameters.add(QuestionPageQuery.PAGE_SIZE);
        parameters.add(query.offset());
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new ProjectQuestionListItemResponse(
                        resultSet.getLong("id"),
                        resultSet.getInt("month"),
                        resultSet.getString("question_text"),
                        resultSet.getInt("status"),
                        resultSet.getString("remark")
                ),
                parameters.toArray()
        );
    }

    @Override
    public Optional<ProjectQuestion> findById(Long questionId) {
        return jdbcTemplate.query(
                "SELECT id, project_id, image_url, image_desc, question_text, month, "
                        + "before_guide, status, remark, del_flag, created_at, updated_at "
                        + "FROM ai4_project_question WHERE id = ? AND del_flag = 1",
                questionRowMapper,
                questionId
        ).stream().findFirst();
    }

    @Override
    public List<QuestionOptionResponse> findOptionsByQuestionId(Long questionId) {
        Map<Long, OptionBuilder> options = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT option_table.id AS option_id, option_table.option_text, "
                        + "option_table.impact_type, option_table.after_guide, "
                        + "option_attribute.attribute_id, sand_attribute.attribute_name, "
                        + "option_attribute.attribute_effect "
                        + "FROM ai4_project_question_option option_table "
                        + "LEFT JOIN ai4_project_question_option_attribute option_attribute "
                        + "ON option_attribute.option_id = option_table.id "
                        + "LEFT JOIN ai4_project_sandtable_attribute sand_attribute "
                        + "ON sand_attribute.id = option_attribute.attribute_id "
                        + "WHERE option_table.question_id = ? "
                        + "ORDER BY option_table.id ASC, option_attribute.id ASC",
                resultSet -> {
                    Long optionId = resultSet.getLong("option_id");
                    OptionBuilder builder = options.computeIfAbsent(
                            optionId,
                            id -> new OptionBuilder(
                                    id,
                                    getString(resultSet, "option_text"),
                                    getString(resultSet, "impact_type"),
                                    resultSetGetNullableString(resultSet, "after_guide"),
                                    new ArrayList<>()
                            )
                    );
                    Long attributeId = getNullableLong(resultSet, "attribute_id");
                    if (attributeId != null) {
                        builder.attributes().add(new QuestionOptionAttributeResponse(
                                attributeId,
                                resultSetGetNullableString(resultSet, "attribute_name"),
                                getString(resultSet, "attribute_effect")
                        ));
                    }
                },
                questionId
        );
        return options.values()
                .stream()
                .map(builder -> new QuestionOptionResponse(
                        builder.optionId(),
                        builder.optionText(),
                        builder.impactType(),
                        builder.afterGuide(),
                        builder.attributes()
                ))
                .toList();
    }

    @Override
    public long countEnabled(Long projectId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai4_project_question "
                        + "WHERE project_id = ? AND status = 0 AND del_flag = 1",
                Long.class,
                projectId
        );
        return count == null ? 0 : count;
    }

    @Override
    public Long save(CreateQuestionRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO ai4_project_question("
                            + "project_id, image_url, image_desc, question_text, month, "
                            + "before_guide, status, remark, del_flag"
                            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, request.projectId());
            statement.setString(2, request.imageUrl().trim());
            statement.setString(3, trimToNull(request.imageDesc()));
            statement.setString(4, request.questionText().trim());
            statement.setInt(5, request.month());
            statement.setString(6, trimToNull(request.beforeGuide()));
            statement.setInt(7, request.status());
            statement.setString(8, trimToNull(request.remark()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to obtain generated question ID");
        }
        return key.longValue();
    }

    @Override
    public int update(Long questionId, UpdateQuestionRequest request) {
        return jdbcTemplate.update(
                "UPDATE ai4_project_question SET image_url = ?, image_desc = ?, "
                        + "question_text = ?, month = ?, before_guide = ?, status = ?, remark = ? "
                        + "WHERE id = ? AND del_flag = 1",
                request.imageUrl().trim(),
                trimToNull(request.imageDesc()),
                request.questionText().trim(),
                request.month(),
                trimToNull(request.beforeGuide()),
                request.status(),
                trimToNull(request.remark()),
                questionId
        );
    }

    @Override
    public void deleteOptions(Long questionId) {
        jdbcTemplate.update(
                "DELETE option_attribute FROM ai4_project_question_option_attribute option_attribute "
                        + "JOIN ai4_project_question_option option_table "
                        + "ON option_table.id = option_attribute.option_id "
                        + "WHERE option_table.question_id = ?",
                questionId
        );
        jdbcTemplate.update(
                "DELETE FROM ai4_project_question_option WHERE question_id = ?",
                questionId
        );
    }

    @Override
    public void insertOption(Long questionId, QuestionOptionRequest option) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO ai4_project_question_option("
                            + "question_id, option_text, impact_type, after_guide"
                            + ") VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, questionId);
            statement.setString(2, option.optionText().trim());
            statement.setString(3, option.impactType().trim());
            statement.setString(4, trimToNull(option.afterGuide()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to obtain generated option ID");
        }
        Long optionId = key.longValue();
        option.attributes().forEach(attribute -> jdbcTemplate.update(
                "INSERT INTO ai4_project_question_option_attribute("
                        + "option_id, attribute_id, attribute_effect"
                        + ") VALUES (?, ?, ?)",
                optionId,
                attribute.attributeId(),
                attribute.attributeEffect().trim()
        ));
    }

    @Override
    public int softDelete(Long questionId) {
        return jdbcTemplate.update(
                "UPDATE ai4_project_question SET del_flag = 2 WHERE id = ? AND del_flag = 1",
                questionId
        );
    }

    private String buildPageWhereSql(
            QuestionPageQuery query,
            List<Object> parameters,
            String selectSql
    ) {
        StringBuilder sql = new StringBuilder(selectSql)
                .append(" WHERE question.project_id = ? AND question.del_flag = 1");
        parameters.add(query.projectId());

        if (hasText(query.questionText())) {
            sql.append(" AND question.question_text LIKE ?");
            parameters.add("%" + query.questionText().trim() + "%");
        }
        if (hasText(query.remark())) {
            sql.append(" AND question.remark LIKE ?");
            parameters.add("%" + query.remark().trim() + "%");
        }
        return sql.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String getString(java.sql.ResultSet resultSet, String columnName) {
        try {
            return resultSet.getString(columnName);
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String resultSetGetNullableString(java.sql.ResultSet resultSet, String columnName) {
        try {
            return resultSet.getString(columnName);
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Long getNullableLong(java.sql.ResultSet resultSet, String columnName) {
        try {
            long value = resultSet.getLong(columnName);
            return resultSet.wasNull() ? null : value;
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record ProjectAttributeBuilder(
            Long projectId,
            String projectName,
            List<QuestionAttributeResponse> attributes
    ) {
    }

    private record OptionBuilder(
            Long optionId,
            String optionText,
            String impactType,
            String afterGuide,
            List<QuestionOptionAttributeResponse> attributes
    ) {
    }
}
