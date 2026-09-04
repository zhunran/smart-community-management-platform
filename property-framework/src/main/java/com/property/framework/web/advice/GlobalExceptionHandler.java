package com.property.framework.web.advice;

import com.property.common.exception.AuthException;
import com.property.common.exception.BusinessException;
import com.property.common.exception.ErrorCode;
import com.property.common.exception.ForbiddenException;
import com.property.common.result.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 捕获各类异常，返回统一格式的 ApiResult
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] code={}, message={}, path={}", e.getCode(), e.getMessage(), request.getRequestURI());
        return ApiResult.error(e.getCode(), e.getMessage()).traceId(getTraceId());
    }

    /**
     * 鉴权异常（401）
     */
    @ExceptionHandler(AuthException.class)
    public ApiResult<?> handleAuthException(AuthException e, HttpServletRequest request) {
        log.warn("[鉴权异常] code={}, message={}, path={}", e.getCode(), e.getMessage(), request.getRequestURI());
        return ApiResult.error(e.getCode(), e.getMessage()).traceId(getTraceId());
    }

    /**
     * 权限不足异常（403）
     */
    @ExceptionHandler(ForbiddenException.class)
    public ApiResult<?> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        log.warn("[权限不足] code={}, message={}, path={}", e.getCode(), e.getMessage(), request.getRequestURI());
        return ApiResult.error(e.getCode(), e.getMessage()).traceId(getTraceId());
    }

    /**
     * 参数校验异常：@Valid 注解校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败] {}", msg);
        return ApiResult.error(ErrorCode.PARAM_ERROR.getCode(), msg).traceId(getTraceId());
    }

    /**
     * 参数校验异常：@BindException（GET 请求 @Valid 绑定失败）
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<?> handleBindException(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[参数绑定失败] {}", msg);
        return ApiResult.error(ErrorCode.PARAM_ERROR.getCode(), msg).traceId(getTraceId());
    }

    /**
     * 参数校验异常：@RequestParam 上 @NotBlank 等校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<?> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数约束失败] {}", msg);
        return ApiResult.error(ErrorCode.PARAM_ERROR.getCode(), msg).traceId(getTraceId());
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResult<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("[缺少请求参数] name={}, type={}", e.getParameterName(), e.getParameterType());
        return ApiResult.error(ErrorCode.BAD_REQUEST.getCode(),
                "缺少必要参数: " + e.getParameterName()).traceId(getTraceId());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResult<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("[参数类型不匹配] name={}, requiredType={}", e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        return ApiResult.error(ErrorCode.BAD_REQUEST.getCode(),
                "参数 " + e.getName() + " 类型不匹配").traceId(getTraceId());
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("[请求方法不支持] method={}, supportedMethods={}", e.getMethod(),
                e.getSupportedHttpMethods());
        return ApiResult.error(ErrorCode.METHOD_NOT_ALLOWED.getCode(), "请求方法不支持").traceId(getTraceId());
    }

    /**
     * 请求体不可读（JSON 格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[请求体不可读] {}", e.getMessage());
        return ApiResult.error(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误").traceId(getTraceId());
    }

    /** 不记录的静态资源路径（浏览器自动请求，无需关注） */
    private static final String[] IGNORED_STATIC_RESOURCES = {
            "favicon.ico", ".well-known"
    };

    /**
     * 资源不存在（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResult<?> handleNoResourceFoundException(NoResourceFoundException e) {
        // 跳过浏览器自动请求的静态资源
        for (String ignored : IGNORED_STATIC_RESOURCES) {
            if (e.getMessage().contains(ignored)) {
                return ApiResult.error(ErrorCode.NOT_FOUND.getCode(), "请求的资源不存在").traceId(getTraceId());
            }
        }
        log.warn("[资源不存在] {}", e.getMessage());
        return ApiResult.error(ErrorCode.NOT_FOUND.getCode(), "请求的资源不存在").traceId(getTraceId());
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<?> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] path={}, message={}", request.getRequestURI(), e.getMessage(), e);
        return ApiResult.error(ErrorCode.INTERNAL_ERROR.getCode(), "服务器内部错误，请稍后重试")
                .traceId(getTraceId());
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "";
    }
}
