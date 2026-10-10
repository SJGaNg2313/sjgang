package com.ai4.partner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新增合伙人请求*/
public record CreatePartnerRequest(
        @NotBlank(message = "合伙人名称不能为空")
        @Size(max = 8, message = "合伙人名称不能超过8个字符") String partnerName,
        @NotBlank(message = "头衔不能为空")
        @Size(max = 6, message = "头衔不能超过6个字符") String title,
        @NotBlank(message = "宣言不能为空")
        @Size(max = 11, message = "宣言不能超过11个字符") String declaration,
        @NotBlank(message = "初始开场白不能为空") String openingMessage,
        @NotBlank(message = "主题色不能为空") String themeColor,
        @NotBlank(message = "形象大图地址不能为空")
        @Size(max = 1024, message = "形象大图地址不能超过1024个字符") String large,
        @NotBlank(message = "形象中图地址不能为空")
        @Size(max = 1024, message = "形象中图地址不能超过1024个字符") String medium,
        @NotBlank(message = "形象小图地址不能为空")
        @Size(max = 1024, message = "形象小图地址不能超过1024个字符") String small,
        @NotBlank(message = "全身图地址不能为空")
        @Size(max = 1024, message = "全身图地址不能超过1024个字符") String fullBody,
        @NotBlank(message = "头像地址不能为空")
        @Size(max = 1024, message = "头像地址不能超过1024个字符") String avatar
) {
}
