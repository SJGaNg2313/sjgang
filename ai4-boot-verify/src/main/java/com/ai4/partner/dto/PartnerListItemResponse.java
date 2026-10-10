package com.ai4.partner.dto;

/** 合伙人分页列表项 */
public record PartnerListItemResponse(
        String partnerId, String partnerName,
        String title, String declaration, String themeColor, String large
) {
}
