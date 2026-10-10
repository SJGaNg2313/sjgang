package com.ai4.common.exception;

import com.ai4.common.response.ApiResponse;
import com.ai4.common.response.FieldErrorItem;
import com.ai4.common.response.ValidationErrorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 将项目异常统一转换为 ApiResponse。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * JSON 结构、字段类型或日期格式无法解析时返回 HTTP 400。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        return ApiResponse.failed(
                ErrorCode.INVALID_REQUEST.code(),
                ErrorCode.INVALID_REQUEST.message()
        );
    }

    /**
     * 处理身份认证失败。
     *
     * HTTP 401 表示请求者尚未通过身份认证，
     * 例如没有 Token、Token 非法或者 Token 已过期。
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleUnauthorizedException(
            UnauthorizedException exception) {

        return ApiResponse.failed(
                exception.code(),
                exception.getMessage()
        );
    }

    /**
     * 用户已经登录，但没有接口访问权限时返回 HTTP 403。
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleForbiddenException(ForbiddenException exception) {
        return ApiResponse.failed(exception.code(), exception.getMessage());
    }

    /**
     * 普通业务异常统一返回 HTTP 400。
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBizException(BizException exception) {
        return ApiResponse.failed(exception.code(), exception.getMessage());
    }

    /**
     * 收集请求参数的字段校验错误。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<ValidationErrorData> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<FieldErrorItem> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorItem(error.getField(), error.getDefaultMessage()))
                .toList();

        return ApiResponse.validationFailed(new ValidationErrorData(fieldErrors));
    }

    /**
     * 兜底处理未知异常，避免暴露内部细节。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ApiResponse.failed(
                //取错误机器码
                ErrorCode.INTERNAL_ERROR.code(),
                //取提示文案
                ErrorCode.INTERNAL_ERROR.message());
    }
}
