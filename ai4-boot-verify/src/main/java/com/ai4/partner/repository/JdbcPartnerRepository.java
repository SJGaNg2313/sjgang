package com.ai4.partner.repository;

import com.ai4.partner.model.Partner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 使用 JdbcTemplate 操作合伙人 */
@Repository
public class JdbcPartnerRepository implements PartnerRepository {

    private static final String COLUMNS = """
            id, partner_name, title, declaration, opening_message, theme_color,
            large, medium, small, full_body, avatar, version, del_flag, created_at, updated_at
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Partner> partnerRowMapper = (resultSet, rowNum) -> new Partner(
            resultSet.getString("id"),
            resultSet.getString("partner_name"),
            resultSet.getString("title"),
            resultSet.getString("declaration"),
            resultSet.getString("opening_message"),
            resultSet.getString("theme_color"),
            resultSet.getString("large"),
            resultSet.getString("medium"),
            resultSet.getString("small"),
            resultSet.getString("full_body"),
            resultSet.getString("avatar"),
            resultSet.getInt("version"),
            resultSet.getInt("del_flag"),
            resultSet.getTimestamp("created_at").toLocalDateTime(),
            resultSet.getTimestamp("updated_at").toLocalDateTime()
    );

    public JdbcPartnerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai4_partner WHERE del_flag = 1",
                Long.class
        );
        return count == null ? 0 : count;
    }

    @Override
    public List<Partner> findPage(int offset, int limit) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS
                        + " FROM ai4_partner WHERE del_flag = 1"
                        + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                partnerRowMapper, limit, offset
        );
    }

    @Override
    public Optional<Partner> findById(String partnerId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS
                        + " FROM ai4_partner WHERE id = ? AND del_flag = 1",
                partnerRowMapper, partnerId
        ).stream().findFirst();
    }

    @Override
    public boolean existsByName(String partnerName, String excludePartnerId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai4_partner WHERE del_flag = 1 "
                        + "AND partner_name = ? AND (? IS NULL OR id <> ?)",
                Integer.class,
                partnerName,
                excludePartnerId,
                excludePartnerId
        );
        return count != null && count > 0;
    }

    @Override
    public int save(Partner partner) {
        return jdbcTemplate.update(
                "INSERT INTO ai4_partner("
                        + "id, partner_name, title, declaration, opening_message, theme_color,"
                        + " large, medium, small, full_body, avatar, version, del_flag"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 1)",
                partner.id(), partner.partnerName(), partner.title(), partner.declaration(),
                partner.openingMessage(), partner.themeColor(), partner.large(),
                partner.medium(), partner.small(), partner.fullBody(), partner.avatar()
        );
    }

    @Override
    public int update(Partner partner, int expectedVersion) {
        return jdbcTemplate.update(
                "UPDATE ai4_partner SET partner_name = ?, title = ?, declaration = ?,"
                        + " opening_message = ?, theme_color = ?, large = ?, medium = ?,"
                        + " small = ?, full_body = ?, avatar = ?, version = version + 1"
                        + " WHERE id = ? AND del_flag = 1 AND version = ?",
                partner.partnerName(), partner.title(), partner.declaration(),
                partner.openingMessage(), partner.themeColor(), partner.large(),
                partner.medium(), partner.small(), partner.fullBody(), partner.avatar(),
                partner.id(),
                expectedVersion
        );
    }

    @Override
    public int softDelete(String partnerId) {
        return jdbcTemplate.update(
                "UPDATE ai4_partner partner SET del_flag = 2"
                        + " WHERE partner.id = ? AND partner.del_flag = 1"
                        + " AND NOT EXISTS ("
                        + " SELECT 1 FROM ai4_project_task_partner relation"
                        + " WHERE relation.partner_id = partner.id"
                        + " )",
                partnerId
        );
    }
}
