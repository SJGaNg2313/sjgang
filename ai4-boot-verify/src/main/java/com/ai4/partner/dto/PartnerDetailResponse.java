package com.ai4.partner.dto;

/** 合伙人完整详情，用于修改表单回填 */
public record PartnerDetailResponse(
        String partnerId, String partnerName, Integer version,
        String title, String declaration, String openingMessage, String themeColor,
        String large, String medium, String small, String fullBody, String avatar
) {
}
