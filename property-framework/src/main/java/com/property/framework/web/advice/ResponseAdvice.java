package com.property.framework.web.advice;

import com.property.common.result.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应体包装
 * 将 Controller 返回的非 ApiResult 数据自动包装为 ApiResult.success()
 * 以下情况跳过包装：
 *   1. 返回值已经是 ApiResult 类型
 *   2. 返回值为 String 类型（由 StringHttpMessageConverter 处理）
 *   3. 返回值为 Resource / 文件流
 *   4. Swagger/SpringDoc 接口路径
 */
@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    /** 不包装的路径前缀 */
    private static final String[] EXCLUDED_PATHS = {
            "/v3/api-docs", "/swagger-ui", "/doc.html", "/webjars"
    };

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 跳过已包装的类型
        Class<?> returnClass = returnType.getMethod() != null
                ? returnType.getMethod().getReturnType()
                : returnType.getParameterType();

        if (ApiResult.class.isAssignableFrom(returnClass)) {
            return false;
        }
        // 跳过 String 类型（由 StringHttpMessageConverter 处理）
        if (returnClass == String.class) {
            return false;
        }
        // 跳过 Resource / 文件下载
        if (Resource.class.isAssignableFrom(returnClass)) {
            return false;
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // 跳过 Swagger/SpringDoc 路径
        String path = request.getURI().getPath();
        for (String excludePath : EXCLUDED_PATHS) {
            if (path.startsWith(excludePath)) {
                return body;
            }
        }

        // 跳过 SSE 流式响应：Flux<String> + text/event-stream 由 ReactiveTypeHandler 逐段写出，
        // 若包装成 ApiResult 会被 StringHttpMessageConverter 强转 String 时抛 ClassCastException
        if (selectedContentType != null && selectedContentType.includes(MediaType.TEXT_EVENT_STREAM)) {
            return body;
        }

        ApiResult<Object> result = ApiResult.success(body);
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            result.traceId(traceId);
        }
        return result;
    }
}
