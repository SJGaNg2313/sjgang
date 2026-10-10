package com.ai4.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 学员账户列表中的单条非敏感数据。
 */
public record AccountListItemResponse(
        String userId,
        String nick,
        String phone,
        String className,
        String packageName,
        String identity,
        String deviceModel,
        String deviceSystemVersion,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime signupTime,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime loginTime
) {
}
