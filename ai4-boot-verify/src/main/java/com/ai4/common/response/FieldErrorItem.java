package com.ai4.common.response;

/**
 * 单个请求字段的校验错误。
 */
public record FieldErrorItem(
        String field,
        String reason
) {
}
