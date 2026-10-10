package com.ai4.dict.model;

import java.time.LocalDateTime;

public record DictItem (
        String id,
        String typeId,
        String itemLabel,
        String itemValue,
        Integer sortOrder,
        Integer status,
        String extra,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
}
