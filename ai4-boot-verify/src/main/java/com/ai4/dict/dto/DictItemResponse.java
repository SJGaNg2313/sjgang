package com.ai4.dict.dto;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/** 字典项响应。 */
public record DictItemResponse(
        String id,
        String typeId,
        String itemLabel,
        String itemValue,
        Integer sortOrder,
        Integer status,
        JsonNode extra,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
