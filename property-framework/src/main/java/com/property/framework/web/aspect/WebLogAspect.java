package com.property.framework.web.aspect;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@RequiredArgsConstructor
@Component
public class WebLogAspect {
    private final ObjectMapper objectMapper;
    @Pointcut("execution(public * com.property..controller..*Controller.*(..))")
    public void webLog(){}

    @Around("webLog()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable
    {
        long start=System.currentTimeMillis();
        String traceId=MDC.get("traceId");
        String method=proceedingJoinPoint.getSignature().toShortString();
        HttpServletRequest request=currentRequest();
        log.info("[请求开始]: traceId={} method={} uri={} args={}",traceId,method,request!=null?request.getRequestURI():null,safeArgs(proceedingJoinPoint.getArgs())  );
        Object result;
        try {
            result=proceedingJoinPoint.proceed();
        }catch (Throwable e)
        {
            log.error("[请求错误]: traceId={} method={} cost={}ms ",traceId,method,System.currentTimeMillis()-start,e);
            throw  e;
        }
        log.info("[请求结束]:traceId={} method={} cost={}ms result={}",traceId,method,System.currentTimeMillis()-start,safeJson(result));
        return result;
    }
    private HttpServletRequest currentRequest()
    {
        ServletRequestAttributes attributes=(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes!=null? attributes.getRequest() : null;
    }
    private String safeArgs(Object[] args)
    {
        if (args==null)
        {
            return "[]";
        }
        String[] parts=new String[args.length];
        for (int i = 0; i < args.length; i++)
        {
            Object arg=args[i];
            // 不能序列化 Servlet 请求/响应对象：Jackson 会读取 getWriter/getOutputStream
            // 导致 getWriter() has already been called，破坏后续正常响应写出
            if (arg instanceof ServletRequest || arg instanceof ServletResponse)
            {
                parts[i]="["+arg.getClass().getSimpleName()+"]";
            }
            else
            {
                parts[i]=safeJson(arg);
            }
        }
        return java.util.Arrays.toString(parts);
    }
    private String safeJson(Object object)
    {
        try {
            return objectMapper.writeValueAsString(object);
        }catch (Exception e)
        {
            return "[servilize fail]";
        }
    }
}
