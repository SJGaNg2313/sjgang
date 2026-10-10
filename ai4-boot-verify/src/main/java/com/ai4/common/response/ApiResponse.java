package com.ai4.common.response;

import com.ai4.common.exception.ErrorCode;

/**
 * 所有 HTTP 接口共用的响应外层结构。
 */
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId
) {

    /**
     * 构造成功响应。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0", "success", data, null);
    }

    /**
     * 构造包含字段错误的参数校验响应。
     */
    public static <T> ApiResponse<T> validationFailed(T data) {
        return new ApiResponse<>(
                ErrorCode.VALIDATION_FAILED.code(),
                ErrorCode.VALIDATION_FAILED.message(),
                data,
                null
        );
    }

    /**
     * 构造不包含业务数据的失败响应。
     */
    public static <T> ApiResponse<T> failed(String code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }
}
