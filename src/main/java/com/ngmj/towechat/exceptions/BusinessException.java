package com.ngmj.towechat.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private Integer code = HttpStatus.INTERNAL_SERVER_ERROR.value();

    // 无参构造函数
    public BusinessException() {
        super();
    }

    // 带消息的构造函数
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }


    // 带消息和原因的构造函数
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    // 带原因的构造函数
    public BusinessException(Throwable cause) {
        super(cause);
    }
}
