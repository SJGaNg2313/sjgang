package com.ai4.common.exception;

/**
 * 表示用户已经登录，但没有执行当前操作的权限。
 */
public class ForbiddenException extends BizException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
