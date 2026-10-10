package com.ai4.partner.model;

import java.time.LocalDateTime;

/** 合伙人持久化数据模型 */
public record Partner(
        String id, String partnerName, String title, String declaration,
        String openingMessage, String themeColor, String large, String medium,
        String small, String fullBody, String avatar, Integer version, Integer delFlag,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
