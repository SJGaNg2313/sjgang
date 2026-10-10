package com.ai4.partner.dto;

public record PartnerImageUploadResponse (
        String large,
        String medium,
        String small,
        String fullBody,
        String avatar
){
}
