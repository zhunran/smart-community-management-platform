package com.property.common.result;

import com.property.common.exception.ErrorCode;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一返回体
 * 所有 Controller 响应统一使用此包装
 */
@Data
public class ApiResult<T> {

    private int code;
    private String msg;
    private T data;
    private String timestamp;
    private String traceId;

    private ApiResult() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ==================== 成功 ====================

    public static <T> ApiResult<T> success() {
        ApiResult<T> result = new ApiResult<>();
        result.code = ErrorCode.SUCCESS.getCode();
        result.msg = ErrorCode.SUCCESS.getMessage();
        return result;
    }

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = ErrorCode.SUCCESS.getCode();
        result.msg = ErrorCode.SUCCESS.getMessage();
        result.data = data;
        return result;
    }

    public static <T> ApiResult<T> success(String msg, T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = ErrorCode.SUCCESS.getCode();
        result.msg = msg;
        result.data = data;
        return result;
    }

    // ==================== 失败 ====================

    public static <T> ApiResult<T> error(int code, String msg) {
        ApiResult<T> result = new ApiResult<>();
        result.code = code;
        result.msg = msg;
        return result;
    }

    public static <T> ApiResult<T> error(ErrorCode errorCode) {
        ApiResult<T> result = new ApiResult<>();
        result.code = errorCode.getCode();
        result.msg = errorCode.getMessage();
        return result;
    }

    public static <T> ApiResult<T> error(ErrorCode errorCode, String msg) {
        ApiResult<T> result = new ApiResult<>();
        result.code = errorCode.getCode();
        result.msg = msg;
        return result;
    }

    public static <T> ApiResult<T> error(String msg) {
        ApiResult<T> result = new ApiResult<>();
        result.code = ErrorCode.INTERNAL_ERROR.getCode();
        result.msg = msg;
        return result;
    }

    // ==================== 便捷方法 ====================

    public ApiResult<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }
}
