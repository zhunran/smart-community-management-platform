package com.property.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 业务错误码（以 1000 开头）
    BUSINESS_ERROR(1000, "业务异常"),
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    PASSWORD_ERROR(1003, "密码错误"),
    CAPTCHA_ERROR(1004, "验证码错误"),
    DATA_EXISTS(1005, "数据已存在"),
    DATA_NOT_EXISTS(1006, "数据不存在"),
    FILE_TOO_LARGE(1007, "文件大小超过限制"),
    FILE_TYPE_ERROR(1008, "文件类型不支持"),
    OPERATION_FAILED(1009, "操作失败"),
    STATUS_ERROR(1010, "状态异常"),
    PARAM_ERROR(1011, "参数校验失败");

    private final int code;
    private final String message;

    /**
     * 根据 code 获取枚举（返回 Optional，避免 NPE）
     */
    public static Optional<ErrorCode> fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return Optional.of(errorCode);
            }
        }
        return Optional.empty();
    }
}
