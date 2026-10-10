package com.ai4.dict.repository;

import com.ai4.dict.model.DictItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 使用 JdbcTemplate 操作字典项表。 */
@Repository
public class JdbcDictItemRepository implements DictItemRepository {

    private static final String COLUMNS = """
            id, type_id, item_label, item_value, sort_order, status,
            CAST(extra AS CHAR) AS extra, remark, created_at, updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DictItem> rowMapper = (resultSet, rowNumber) -> new DictItem(
            resultSet.getString("id"),
            resultSet.getString("type_id"),
            resultSet.getString("item_label"),
            resultSet.getString("item_value"),
            resultSet.getInt("sort_order"),
            resultSet.getInt("status"),
            resultSet.getString("extra"),
            resultSet.getString("remark"),
            resultSet.getTimestamp("created_at").toLocalDateTime(),
            resultSet.getTimestamp("updated_at").toLocalDateTime()
    );

    public JdbcDictItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<DictItem> findByTypeId(
            String typeId,
            String itemLabel,
            String itemValue,
            Integer status
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT " + COLUMNS + " FROM sys_dict_item WHERE type_id = ?"
        );
        List<Object> parameters = new ArrayList<>();
        parameters.add(typeId);
        if (hasText(itemLabel)) {
            sql.append(" AND item_label LIKE ?");
            parameters.add("%" + itemLabel.trim() + "%");
        }
        if (hasText(itemValue)) {
            sql.append(" AND item_value = ?");
            parameters.add(itemValue.trim());
        }
        if (status != null) {
            sql.append(" AND status = ?");
            parameters.add(status);
        }
        sql.append(" ORDER BY sort_order ASC, created_at ASC, id ASC");
        return jdbcTemplate.query(sql.toString(), rowMapper, parameters.toArray());
    }

    @Override
    public Optional<DictItem> findById(String itemId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM sys_dict_item WHERE id = ?",
                rowMapper,
                itemId
        ).stream().findFirst();
    }

    @Override
    public boolean existsByValue(String typeId, String itemValue, String excludedItemId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_item"
                        + " WHERE type_id = ? AND item_value = ?"
                        + " AND (? IS NULL OR id <> ?)",
                Integer.class,
                typeId,
                itemValue,
                excludedItemId,
                excludedItemId
        );
        return count != null && count > 0;
    }

    @Override
    public int save(DictItem item) {
        return jdbcTemplate.update(
                "INSERT INTO sys_dict_item("
                        + "id, type_id, item_label, item_value, sort_order, status, extra, remark"
                        + ") VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?)",
                item.id(), item.typeId(), item.itemLabel(), item.itemValue(),
                item.sortOrder(), item.status(), item.extra(), item.remark()
        );
    }

    @Override
    public int update(DictItem item) {
        return jdbcTemplate.update(
                "UPDATE sys_dict_item SET item_label = ?, item_value = ?, sort_order = ?,"
                        + " status = ?, extra = CAST(? AS JSON), remark = ? WHERE id = ?",
                item.itemLabel(), item.itemValue(), item.sortOrder(), item.status(),
                item.extra(), item.remark(), item.id()
        );
    }

    @Override
    public int deleteById(String itemId) {
        return jdbcTemplate.update("DELETE FROM sys_dict_item WHERE id = ?", itemId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
