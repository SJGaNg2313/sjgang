package com.ai4.common.response;

import java.util.List;

/**
 * 参数校验失败时返回的字段错误集合。
 */
public record ValidationErrorData(
        List<FieldErrorItem> fieldErrors
) {
}
