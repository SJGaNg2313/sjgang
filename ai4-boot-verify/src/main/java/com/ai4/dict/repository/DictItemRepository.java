package com.ai4.dict.repository;

import com.ai4.dict.model.DictItem;

import java.util.List;
import java.util.Optional;

/** 字典项数据访问边界。 */
public interface DictItemRepository {

    List<DictItem> findByTypeId(String typeId, String itemLabel, String itemValue, Integer status);

    Optional<DictItem> findById(String itemId);

    boolean existsByValue(String typeId, String itemValue, String excludedItemId);

    int save(DictItem item);

    int update(DictItem item);

    int deleteById(String itemId);
}
