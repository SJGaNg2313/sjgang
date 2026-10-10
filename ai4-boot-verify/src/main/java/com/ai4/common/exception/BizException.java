package com.ai4.common.exception;

/**
 * 表示可以安全返回给客户端的业务异常。
 */
public class BizException extends RuntimeException{

    private final String code;

    public BizException(ErrorCode errorCode){
        super(errorCode.message());
        this.code = errorCode.code();
    }

    public BizException(String code, String message){
        super(message);
        this.code = code;
    }

    public String code(){
        return code;
    }
}
