package com.property.common.exception;

import lombok.Getter;

/**
 * 权限不足异常（403）
 * 已登录但无权限访问资源时抛出
 */
@Getter
public class ForbiddenException extends RuntimeException {

    private final int code;

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN.getMessage());
        this.code = ErrorCode.FORBIDDEN.getCode();
    }

    public ForbiddenException(String message) {
        super(message);
        this.code = ErrorCode.FORBIDDEN.getCode();
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.FORBIDDEN.getCode();
    }
}
