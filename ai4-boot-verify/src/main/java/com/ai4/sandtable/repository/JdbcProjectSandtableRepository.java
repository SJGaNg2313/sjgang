package com.ai4.sandtable.repository;

import com.ai4.sandtable.dto.SandtableAttributeRequest;
import com.ai4.sandtable.dto.SandtableGainRequest;
import com.ai4.sandtable.dto.SandtableSegmentRequest;
import com.ai4.sandtable.model.ProjectSandtable;
import com.ai4.sandtable.model.SandtableAttribute;
import com.ai4.sandtable.model.SandtableGain;
import com.ai4.sandtable.model.SandtableSegment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 使用JdbcTemplate操作项目沙盘三张表。 */
@Repository
public class JdbcProjectSandtableRepository implements ProjectSandtableRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProjectSandtableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ProjectSandtable> findByProjectId(Long projectId) {
        return jdbcTemplate.query(
                "SELECT id, project_id, video_url, trend_url, created_at, updated_at"
                        + " FROM ai4_project_sandtable WHERE project_id = ?",
                (resultSet, rowNumber) -> new ProjectSandtable(
                        resultSet.getLong("id"),
                        resultSet.getLong("project_id"),
                        resultSet.getString("video_url"),
                        resultSet.getString("trend_url"),
                        resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getTimestamp("updated_at").toLocalDateTime()
                ),
                projectId
        ).stream().findFirst();
    }

    @Override
    public List<SandtableSegment> findSegments(Long sandtableId) {
        return jdbcTemplate.query(
                "SELECT id, sandtable_id, segment_time, segment_name, created_at, updated_at"
                        + " FROM ai4_project_sandtable_segment"
                        + " WHERE sandtable_id = ?"
                        + " ORDER BY segment_time IS NULL, segment_time ASC, id ASC",
                (resultSet, rowNumber) -> new SandtableSegment(
                        resultSet.getLong("id"),
                        resultSet.getLong("sandtable_id"),
                        resultSet.getBigDecimal("segment_time"),
                        resultSet.getString("segment_name"),
                        resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getTimestamp("updated_at").toLocalDateTime()
                ),
                sandtableId
        );
    }

    @Override
    public List<SandtableAttribute> findAttributes(Long sandtableId) {
        return jdbcTemplate.query(
                "SELECT id, sandtable_id, attribute_name, icon, `target`, `initial`,"
                        + " created_at, updated_at FROM ai4_project_sandtable_attribute"
                        + " WHERE sandtable_id = ? ORDER BY id ASC",
                (resultSet, rowNumber) -> new SandtableAttribute(
                        resultSet.getLong("id"),
                        resultSet.getLong("sandtable_id"),
                        resultSet.getString("attribute_name"),
                        resultSet.getString("icon"),
                        resultSet.getBigDecimal("target"),
                        resultSet.getBigDecimal("initial"),
                        resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getTimestamp("updated_at").toLocalDateTime()
                ),
                sandtableId
        );
    }

    @Override
    public List<SandtableGain> findGains(Long sandtableId) {
        return jdbcTemplate.query(
                "SELECT id, sandtable_id, icon, gain, created_at, updated_at"
                        + " FROM ai4_project_sandtable_gain"
                        + " WHERE sandtable_id = ? ORDER BY id ASC",
                (resultSet, rowNumber) -> new SandtableGain(
                        resultSet.getLong("id"),
                        resultSet.getLong("sandtable_id"),
                        resultSet.getString("icon"),
                        resultSet.getString("gain"),
                        resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getTimestamp("updated_at").toLocalDateTime()
                ),
                sandtableId
        );
    }

    @Override
    public ProjectSandtable save(Long projectId, String videoUrl, String trendUrl) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO ai4_project_sandtable("
                            + "project_id, video_url, trend_url) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, projectId);
            statement.setString(2, videoUrl);
            statement.setString(3, trendUrl);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to obtain generated sandtable ID");
        }
        return findByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException("Failed to query saved sandtable"));
    }

    @Override
    public int updateMain(Long sandtableId, String videoUrl, String trendUrl) {
        return jdbcTemplate.update(
                "UPDATE ai4_project_sandtable SET video_url = ?, trend_url = ? WHERE id = ?",
                videoUrl,
                trendUrl,
                sandtableId
        );
    }

    @Override
    public void replaceSegments(Long sandtableId, List<SandtableSegmentRequest> segments) {
        jdbcTemplate.update(
                "DELETE FROM ai4_project_sandtable_segment WHERE sandtable_id = ?",
                sandtableId
        );
        for (SandtableSegmentRequest segment : segments) {
            jdbcTemplate.update(
                    "INSERT INTO ai4_project_sandtable_segment("
                            + "sandtable_id, segment_time, segment_name) VALUES (?, ?, ?)",
                    sandtableId,
                    segment.segmentTime(),
                    segment.segmentName()
            );
        }
    }

    @Override
    public void insertAttribute(Long sandtableId, SandtableAttributeRequest attribute) {
        jdbcTemplate.update(
                "INSERT INTO ai4_project_sandtable_attribute("
                        + "sandtable_id, attribute_name, icon, `target`, `initial`)"
                        + " VALUES (?, ?, ?, ?, ?)",
                sandtableId,
                attribute.attributeName(),
                attribute.icon(),
                attribute.target(),
                attribute.initial()
        );
    }

    @Override
    public int updateAttribute(Long sandtableId, SandtableAttributeRequest attribute) {
        return jdbcTemplate.update(
                "UPDATE ai4_project_sandtable_attribute"
                        + " SET attribute_name = ?, icon = ?, `target` = ?, `initial` = ?"
                        + " WHERE id = ? AND sandtable_id = ?",
                attribute.attributeName(),
                attribute.icon(),
                attribute.target(),
                attribute.initial(),
                attribute.id(),
                sandtableId
        );
    }

    @Override
    public boolean hasQuestionReferences(Collection<Long> attributeIds) {
        if (attributeIds.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(
                attributeIds.size(),
                "?"
        ));
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai4_project_question_option_attribute"
                        + " WHERE attribute_id IN (" + placeholders + ")",
                Long.class,
                attributeIds.toArray()
        );
        return count != null && count > 0;
    }

    @Override
    public void deleteAttributes(Long sandtableId, Collection<Long> attributeIds) {
        deleteChildrenByIds("ai4_project_sandtable_attribute", sandtableId, attributeIds);
    }

    @Override
    public void insertGain(Long sandtableId, SandtableGainRequest gain) {
        jdbcTemplate.update(
                "INSERT INTO ai4_project_sandtable_gain(sandtable_id, icon, gain)"
                        + " VALUES (?, ?, ?)",
                sandtableId,
                gain.icon(),
                gain.gain()
        );
    }

    @Override
    public void deleteGains(Long sandtableId, Collection<Long> gainIds) {
        deleteChildrenByIds("ai4_project_sandtable_gain", sandtableId, gainIds);
    }

    private void deleteChildrenByIds(
            String tableName,
            Long sandtableId,
            Collection<Long> ids
    ) {
        if (ids.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        Object[] parameters = new Object[ids.size() + 1];
        parameters[0] = sandtableId;
        int index = 1;
        for (Long id : ids) {
            parameters[index++] = id;
        }
        jdbcTemplate.update(
                "DELETE FROM " + tableName
                        + " WHERE sandtable_id = ? AND id IN (" + placeholders + ")",
                parameters
        );
    }
}
