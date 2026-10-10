package com.ai4.dict.service;

import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import com.ai4.dict.dto.CreateDictItemRequest;
import com.ai4.dict.dto.DictItemResponse;
import com.ai4.dict.dto.UpdateDictItemRequest;
import com.ai4.dict.model.DictItem;
import com.ai4.dict.repository.DictItemRepository;
import com.ai4.dict.repository.DictTypeRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

/** 字典项查询和维护业务。 */
@Service
public class DictItemService {

    private final DictItemRepository itemRepository;
    private final DictTypeRepository typeRepository;
    private final ObjectMapper objectMapper;

    public DictItemService(
            DictItemRepository itemRepository,
            DictTypeRepository typeRepository,
            ObjectMapper objectMapper
    ) {
        this.itemRepository = itemRepository;
        this.typeRepository = typeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<DictItemResponse> list(
            String typeId,
            String itemLabel,
            String itemValue,
            Integer status
    ) {
        requireType(typeId);
        return itemRepository.findByTypeId(typeId, itemLabel, itemValue, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DictItemResponse get(String itemId) {
        return toResponse(findItem(itemId));
    }

    @Transactional
    public DictItemResponse create(String typeId, CreateDictItemRequest request) {
        requireType(typeId);
        String itemValue = request.itemValue().trim();
        ensureValueAvailable(typeId, itemValue, null);
        DictItem item = new DictItem(
                UUID.randomUUID().toString(),
                typeId,
                request.itemLabel().trim(),
                itemValue,
                request.sortOrder() == null ? 0 : request.sortOrder(),
                request.status() == null ? 0 : request.status(),
                writeExtra(request.extra()),
                trimToNull(request.remark()),
                null,
                null
        );
        try {
            itemRepository.save(item);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.DICT_ITEM_VALUE_ALREADY_EXISTS);
        }
        return get(item.id());
    }

    @Transactional
    public DictItemResponse update(String itemId, UpdateDictItemRequest request) {
        DictItem existing = findItem(itemId);
        String itemValue = request.itemValue().trim();
        ensureValueAvailable(existing.typeId(), itemValue, itemId);
        DictItem updated = new DictItem(
                existing.id(),
                existing.typeId(),
                request.itemLabel().trim(),
                itemValue,
                request.sortOrder(),
                request.status(),
                writeExtra(request.extra()),
                trimToNull(request.remark()),
                existing.createdAt(),
                existing.updatedAt()
        );
        try {
            if (itemRepository.update(updated) == 0) {
                throw new BizException(ErrorCode.DICT_ITEM_NOT_FOUND);
            }
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.DICT_ITEM_VALUE_ALREADY_EXISTS);
        }
        return get(itemId);
    }

    @Transactional
    public void delete(String itemId) {
        findItem(itemId);
        if (itemRepository.deleteById(itemId) == 0) {
            throw new BizException(ErrorCode.DICT_ITEM_NOT_FOUND);
        }
    }

    private DictItem findItem(String itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new BizException(ErrorCode.DICT_ITEM_NOT_FOUND));
    }

    private void requireType(String typeId) {
        typeRepository.findById(typeId)
                .orElseThrow(() -> new BizException(ErrorCode.DICT_TYPE_NOT_FOUND));
    }

    private void ensureValueAvailable(String typeId, String itemValue, String excludedItemId) {
        if (itemRepository.existsByValue(typeId, itemValue, excludedItemId)) {
            throw new BizException(ErrorCode.DICT_ITEM_VALUE_ALREADY_EXISTS);
        }
    }

    private DictItemResponse toResponse(DictItem item) {
        return new DictItemResponse(
                item.id(), item.typeId(), item.itemLabel(), item.itemValue(),
                item.sortOrder(), item.status(), readExtra(item.extra()), item.remark(),
                item.createdAt(), item.updatedAt()
        );
    }

    private String writeExtra(JsonNode extra) {
        return extra == null || extra.isNull() ? null : extra.toString();
    }

    private JsonNode readExtra(String extra) {
        if (extra == null || extra.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(extra);
        } catch (JacksonException exception) {
            throw new IllegalStateException("字典项扩展信息不是合法JSON", exception);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
