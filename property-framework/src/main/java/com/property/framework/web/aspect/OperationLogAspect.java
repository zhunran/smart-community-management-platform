package com.property.framework.web.aspect;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.property.common.dto.LoginUser;
import com.property.common.result.ApiResult;
import com.property.framework.entity.SysOperationLogEntity;
import com.property.framework.repository.SysOperationLogMapper;
import com.property.framework.web.annotation.OperationLog;
import com.property.framework.web.security.SecurityUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private final SysOperationLogMapper sysOperationLogMapper;
    private final ObjectMapper objectMapper;

    //异步落库专用线程
    private static final ExecutorService LOG_EXECUTOR=
            Executors.newSingleThreadExecutor(r -> {
                Thread t=new Thread(r,"operation_log");
                t.setDaemon(true);
                return t;
            });
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, OperationLog operationLog) throws Throwable
    {
        long start=System.currentTimeMillis();
        Object res=null;
        Throwable err=null;
        // 在主线程采集上下文快照，异步线程中 RequestContextHolder / ThreadLocal 均不可用
        HttpServletRequest request=currentRequest();
        String traceId=MDC.get("traceId");
        LoginUser loginUser=SecurityUtil.getLoginUser();
        try {
            res=proceedingJoinPoint.proceed();
        }catch (Throwable e)
        {
            err=e;
            throw e;
        }finally {
            final Object result=res;
            final Throwable error=err;
            final long cost=System.currentTimeMillis()-start;
            LOG_EXECUTOR.execute(
                    ()->saveLog(proceedingJoinPoint,operationLog,result,error,cost,request,traceId,loginUser));
        }
        return res;
    }

    private void saveLog(ProceedingJoinPoint proceedingJoinPoint, OperationLog operationLog, Object result,
                         Throwable error, long cost, HttpServletRequest request, String traceId, LoginUser loginUser) {
        try {
            int status=error==null?1:0;
            ApiResult<?> apiResult=result instanceof ApiResult<?> ?(ApiResult<?>) result:null;
            Integer code=apiResult!=null?apiResult.getCode():(status==1?200:500);
            SysOperationLogEntity entity=new SysOperationLogEntity();
            entity.setTraceId(traceId);
            entity.setModule(operationLog.module());
            entity.setAction(operationLog.action());
            entity.setRequestMethod(request!=null?request.getMethod():"-");
            entity.setRequestUrl(request!=null?request.getRequestURI():"-");
            entity.setResponseData(error!=null?truncate(error.getMessage()):truncate(safeJson(result)));
            entity.setIpAddress(ip(request));
            entity.setUserAgent(request!=null?request.getHeader("User-Agent"):"-");
            entity.setCostTime(cost);
            entity.setResultMsg(error!=null?error.getMessage():"成功");
            entity.setStatus(status);
            entity.setResultCode(code);
            entity.setCreateTime(LocalDateTime.now());

            if (loginUser!=null)
            {
                entity.setUserId(loginUser.getUserId());
                entity.setUserName(loginUser.getUsername());
                entity.setRealName(loginUser.getRealName());
            }
            sysOperationLogMapper.insert(entity);
        }catch (Throwable e)
        {
            log.info("落库失败",e);
        }
    }
    private HttpServletRequest currentRequest()
    {
        ServletRequestAttributes attributes=(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes!=null?attributes.getRequest():null;
    }
    private String ip(HttpServletRequest request)
    {
        if (request==null)
        {
            return null;
        }
        String fwd=request.getHeader("X-Forwarded-For");
        return fwd != null ? fwd.split(",")[0].trim() : request.getRemoteAddr();
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
    private String truncate(String s)
    {
        if (s==null)
        {
            return null;
        }
        return s.length()>2000?s.substring(0,2000):s;
    }
}
