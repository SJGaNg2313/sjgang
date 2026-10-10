package com.ai4.dict.repository;

import com.ai4.dict.model.DictType;

import java.util.List;
import java.util.Optional;

public interface DictTypeRepository {
    /**
     * 根据可选条件查询字典类型
     */
    List<DictType> findAll(
            String dictName,
            String dictCode,
            Integer status
    );

    /**
     * 根据主键id查询字典类型
     */
    Optional<DictType> findById(
            String typeId
    );

    /**
     * 判断字典编码是否存在
     */
    boolean existByDictCode(
            String dictCode
    );

    /**
     * 创建字典类型
     */
    int save(
            DictType dictType
    );

    /**
     * 修改字典类型
     */
    int update(
            DictType dictType
    );

    /**
     * 根据字典编码查询
     */
    Optional<DictType> findByDictCode(String dictCode);

}
