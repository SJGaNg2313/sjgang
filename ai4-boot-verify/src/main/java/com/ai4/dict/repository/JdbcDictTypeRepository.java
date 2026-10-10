package com.ai4.dict.repository;

import com.ai4.dict.model.DictType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcDictTypeRepository implements DictTypeRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DictType> dictTypeRowMapper =
            (resultSet, rowNum) -> new DictType(
                    resultSet.getString("id"),
                    resultSet.getString("dict_name"),
                    resultSet.getString("dict_code"),
                    resultSet.getInt("status"),
                    resultSet.getString("remark"),
                    resultSet.getTimestamp("created_at").toLocalDateTime(),
                    resultSet.getTimestamp("updated_at").toLocalDateTime()
            );

    public JdbcDictTypeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据id查询字典类型
     */
    @Override
    public Optional<DictType> findById(String typeId) {
        List<DictType> dictTypes = jdbcTemplate.query(
                "SELECT id, dict_name, dict_code, status, remark, created_at, updated_at FROM sys_dict_type WHERE id = ?",
                dictTypeRowMapper,
                typeId
        );
        return dictTypes.stream().findFirst();
    }

    /**
     * 判断字典编码是否存在
     */
    @Override
    public boolean existByDictCode(String dictCode){
        Integer count = jdbcTemplate.queryForObject(
               "SELECT COUNT(*) FROM sys_dict_type WHERE dict_code = ?",
               Integer.class,
               dictCode
        );
        return count != null && count > 0;
    }

    /**
     * 根据可选条件查询字典类型
     */
    @Override
    public List<DictType> findAll(
            String dictName,
            String dictCode,
            Integer status
            ){
        StringBuilder sql = new StringBuilder(
                "SELECT id,dict_name,dict_code,status,remark,created_at,updated_at FROM sys_dict_type WHERE 1 = 1"
        );

        List<Object> parameters = new ArrayList<>();

        if(dictName != null && !dictName.isBlank()){
            sql.append(" AND dict_name LIKE ?");
            parameters.add("%" + dictName.trim() + "%");
        }

        if (dictCode != null && !dictCode.isBlank()) {
            sql.append(" AND dict_code = ?");
            parameters.add(dictCode.trim());
        }

        if (status != null) {
            sql.append(" AND status = ?");
            parameters.add(status);
        }

        sql.append(" ORDER BY created_at DESC, id");

        return jdbcTemplate.query(
                sql.toString(),
                dictTypeRowMapper,
                parameters.toArray()
        );
    }

    /**
     *创建字典类型
     */
    @Override
    public int save(DictType dictType){
        return jdbcTemplate.update(
                "INSERT INTO sys_dict_type(id,dict_name,dict_code,status,remark) VALUES (?,?,?,?,?)",
                dictType.id(),
                dictType.dictName(),
                dictType.dictCode(),
                dictType.status(),
                dictType.remark()
        );
    }

    /**
     * 修改字典类型
     */
    @Override
    public int update(DictType dictType){
        return jdbcTemplate.update(
                "UPDATE sys_dict_type SET dict_name = ?,remark = ? WHERE id = ?",
                dictType.dictName(),
                dictType.remark(),
                dictType.id()
        );
    }

    /**
     * 根据字典编码查询
     */
    @Override
    public Optional<DictType> findByDictCode(String dictCode){
        List<DictType> dictTypes = jdbcTemplate.query(
                "SELECT id,dict_name,dict_code,status,remark,created_at,updated_at FROM sys_dict_type WHERE dict_code = ?",
                dictTypeRowMapper,
                dictCode
        );
        return dictTypes.stream().findFirst();
    }
}
