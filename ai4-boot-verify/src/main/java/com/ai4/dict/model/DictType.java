package com.ai4.dict.model;

import java.time.LocalDateTime;

public record DictType (
        String id,
        String dictName,
        String dictCode,
        Integer status,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
}
