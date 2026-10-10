package com.ai4.common.exception;

/**
 * 表示用户认证失败
 *
 * 例:1.没有携带token
 * 2.token格式错误
 * 3.token已经过期
 */
public class UnauthorizedException extends BizException{

    public UnauthorizedException(ErrorCode errorCode){
        super(errorCode);
    }
}
