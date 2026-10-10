package com.ai4.dict.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.dict.dto.DictOptionResponse;
import com.ai4.dict.model.DictItem;
import com.ai4.dict.model.DictType;
import com.ai4.dict.repository.DictItemRepository;
import com.ai4.dict.repository.DictTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class DictQueryService {
    private final DictTypeRepository dictTypeRepository;
    private final DictItemRepository dictItemRepository;
    private final ObjectMapper objectMapper;

    public DictQueryService(
            DictTypeRepository dictTypeRepository,
            DictItemRepository dictItemRepository,
            ObjectMapper objectMapper
    ){
        this.dictItemRepository = dictItemRepository;
        this.dictTypeRepository = dictTypeRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据字典编码查询可供业务使用的字典项
     */
    @Transactional(readOnly = true)
    public List<DictOptionResponse> getOptions(String dictCode){
        DictType dictType = dictTypeRepository.findByDictCode(dictCode)
                .filter(type -> type.status() == 0)
                .orElseThrow(() -> new BizException(ErrorCode.DICT_TYPE_NOT_FOUND));

        return dictItemRepository.findByTypeId(
                dictType.id(),
                null,
                null,
                0
        )
                .stream()
                .map(this::toOptionResponse)
                .toList();
    }

    private DictOptionResponse toOptionResponse(DictItem item) {
        return new DictOptionResponse(
                item.itemLabel(),
                item.itemValue(),
                parseExtra(item.extra())
        );
    }

    private JsonNode parseExtra(String extra) {
        if (extra == null || extra.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(extra);
        } catch (JacksonException exception) {
            throw new IllegalStateException("字典项扩展信息不是合法 JSON", exception);
        }
    }
}
