package com.ai4.role.model;

import java.time.LocalDateTime;

public record Role (
        Long roleId,
        String roleName,
        String roleCode,
        Integer status,
        Integer roleSort,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
}
