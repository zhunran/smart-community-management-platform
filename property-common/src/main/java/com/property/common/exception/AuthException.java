package com.property.common.exception;

import lombok.Getter;

/**
 * 鉴权异常（401）
 * 未登录、Token 过期或无效时抛出
 */
@Getter
public class AuthException extends RuntimeException {

    private final int code;

    public AuthException() {
        super(ErrorCode.UNAUTHORIZED.getMessage());
        this.code = ErrorCode.UNAUTHORIZED.getCode();
    }

    public AuthException(String message) {
        super(message);
        this.code = ErrorCode.UNAUTHORIZED.getCode();
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.UNAUTHORIZED.getCode();
    }
}
