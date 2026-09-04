# AOP 实现方案：接口日志切面 + 操作审计切面

> 两个 AOP 均放在 `property-framework` 模块（横切基础设施层）。
> `property-admin-api` 和 `property-owner-api` 的 `scanBasePackages` 都包含 `com.property.framework`，因此一处实现，两端应用同时生效。

---

## 0. 前置：添加 AOP 依赖

当前 `property-framework/pom.xml` 没有 `spring-boot-starter-aop`。自定义 `@Aspect` 解析 AspectJ 表达式需要 `aspectjweaver`，必须添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

> 说明：现有 `@Transactional` / `@Cacheable` 走的是 Spring `Advisor`，不依赖 aspectjweaver；但**自定义 `@Aspect` 必须有 aspectjweaver**。

---

## 一、接口日志切面（WebLogAspect）

- 作用：记录所有 Controller 接口的请求开始 / 结束、耗时、参数、结果、异常。
- 位置：`com.property.framework.web.aspect.WebLogAspect`
- 命中点：`com.property..controller..*Controller`，同时覆盖管理端与业主端。

```java
package com.property.framework.web.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class WebLogAspect {

    /** 直接注入全局 @Primary 的 ObjectMapper（JacksonConfig 已配置 Long→String、日期格式等） */
    private final ObjectMapper objectMapper;

    /** 命中两个应用的 Controller */
    @Pointcut("execution(public * com.property..controller..*Controller.*(..))")
    public void webLog() { }

    @Around("webLog()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");   // 复用 TraceFilter 写入的 traceId
        String method = pjp.getSignature().toShortString();

        HttpServletRequest req = currentRequest();
        log.info("[请求开始] traceId={} method={} uri={} args={}",
                traceId, method,
                req != null ? req.getRequestURI() : "-",
                safeJson(pjp.getArgs()));

        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable e) {
            log.error("[请求异常] traceId={} method={} cost={}ms",
                    traceId, method, System.currentTimeMillis() - start, e);
            throw e;   // 抛给 GlobalExceptionHandler 统一处理，不二次包装
        }

        log.info("[请求结束] traceId={} method={} cost={}ms result={}",
                traceId, method, System.currentTimeMillis() - start, safeJson(result));
        return result;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String safeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return "[serialize failed]"; }
    }
}
```

### 关键设计点
- **命中点覆盖**：`com.property..controller..*Controller` 同时匹配 `com.property.adminapi.controller` 与 `com.property.ownerapi.controller`。
- **不吞异常**：`catch` 记录后 `throw e`，交给 [GlobalExceptionHandler](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/advice/GlobalExceptionHandler.java) 兜底。
- **traceId 协同**：`TraceFilter` 已在请求前置写入 MDC `traceId`，切面取同源，与异常、审计共用一条链路。
- **ObjectMapper**：直接注入，无需重写（见第三节说明）。

---

## 二、操作审计切面（OperationLogAspect）

### 背景
数据库已有设计好的操作日志表 `t_sys_operation_log`（见 [property_management.sql#L1162](file:///d:/.workspace/javaproject/property-management-system/property-management/sql/property_management.sql#L1162)），字段齐全。本方案只差代码层。

### 1. 自定义注解 `@OperationLog`

```java
package com.property.framework.web.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作模块，如：业主管理 */
    String module() default "";
    /** 操作动作，如：新增业主 */
    String action() default "";
}
```

### 2. 实体 `SysOperationLogEntity`（对齐表字段）

```java
package com.property.framework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_sys_operation_log")
public class SysOperationLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private Long userId;
    private String userName;
    private String realName;
    private String module;
    private String action;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String responseData;
    private String ipAddress;
    private String userAgent;
    private Long costTime;
    private Integer resultCode;
    private String resultMsg;
    private Integer status;   // 1成功 0失败
    private LocalDateTime createTime;
}
```

### 3. Mapper `SysOperationLogMapper`

```java
package com.property.framework.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.framework.entity.SysOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLogEntity> {
}
```

### 4. 审计切面

```java
package com.property.framework.web.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.property.common.result.ApiResult;
import com.property.framework.entity.SysOperationLogEntity;
import com.property.framework.repository.SysOperationLogMapper;
import com.property.framework.web.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper operationLogMapper;
    /** 直接注入全局 @Primary 的 ObjectMapper */
    private final ObjectMapper objectMapper;

    /** 异步落库专用线程池：单队列串行写，避免打爆数据库 */
    private static final ExecutorService LOG_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "operation-log");
                t.setDaemon(true);
                return t;
            });

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            result = pjp.proceed();
        } catch (Throwable e) {
            error = e;
            throw e;   // 抛给 GlobalExceptionHandler 处理，审计仍会记录
        } finally {
            final Object res = result;
            final Throwable err = error;
            final long cost = System.currentTimeMillis() - start;
            LOG_EXECUTOR.execute(() -> saveLog(pjp, opLog, res, err, cost));
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint pjp, OperationLog opLog,
                         Object result, Throwable error, long cost) {
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            HttpServletRequest req = currentRequest();
            int status = error == null ? 1 : 0;
            ApiResult<?> apiResult = result instanceof ApiResult<?> ? (ApiResult<?>) result : null;
            Integer code = apiResult != null ? apiResult.getCode() : (status == 1 ? 200 : 500);

            SysOperationLogEntity entity = new SysOperationLogEntity();
            entity.setTraceId(MDC.get("traceId"));
            entity.setModule(opLog.module());
            entity.setAction(opLog.action());
            entity.setRequestMethod(req == null ? "-" : req.getMethod());
            entity.setRequestUrl(req == null ? "-" : req.getRequestURI());
            entity.setRequestParams(truncate(safeJson(pjp.getArgs())));
            entity.setResponseData(error != null ? truncate(error.getMessage()) : truncate(safeJson(result)));
            entity.setIpAddress(ip(req));
            entity.setUserAgent(req == null ? "-" : req.getHeader("User-Agent"));
            entity.setCostTime(cost);
            entity.setResultMsg(error != null ? error.getMessage() : "成功");
            entity.setResultCode(code);
            entity.setStatus(status);
            entity.setCreateTime(LocalDateTime.now());

            // 从 SecurityUtil 取操作人（未登录时不阻塞日志）
            try {
                var user = com.property.framework.web.security.SecurityUtil.requireUser();
                entity.setUserId(user.getUserId());
                entity.setUserName(user.getUsername());
                entity.setRealName(user.getRealName());
            } catch (Exception ignored) { }

            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("操作审计落库失败", e);
        }
    }

    private String safeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return "[serialize failed]"; }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String ip(HttpServletRequest req) {
        if (req == null) return null;
        String fwd = req.getHeader("X-Forwarded-For");
        return fwd != null ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
```

### 5. 使用方式（标在 Controller 方法上）

```java
@OperationLog(module = "业主管理", action = "新增业主")
@PostMapping("/owner")
public ApiResult<?> create(@Valid @RequestBody OwnerCreateRequest req) { ... }
```

### 关键设计决策

| 决策点 | 方案 | 原因 |
|---|---|---|
| 存放模块 | `property-framework` | 横切能力 + 双应用复用；已有实体/Mapper 先例 |
| 命中方式 | `@annotation(opLog)` 切自定义注解 | 只记录"想审计"的方法，可配 module/action 语义化名称 |
| 成功/失败都记 | `@Around` + `finally` | 审计要追溯失败，`@AfterReturning` 只记成功 |
| 异步落库 | 单线程池 → insert | 不阻塞业务请求；串行写防打爆库 |
| 事务解耦 | `pjp.proceed()` 之后异步 save，在业务事务外 | 业务回滚不影响审计留痕 |
| 异常透传 | catch 后 `throw e` | 交给 GlobalExceptionHandler 统一返回 |
| 0 依赖追踪 | 复用 TraceFilter 的 MDC.traceId | 审计、异常、日志共用一条链路 |

---

## 三、关于 ObjectMapper（FAQ）

**问题：日志 AOP 里的 ObjectMapper 是什么？需要单独重写吗？**

- **是什么**：`ObjectMapper` 是 Jackson 的核心类，做 Java 对象 ↔ JSON 字符串互转。日志/审计切面用它把入参、响应转成字符串存库（`t_sys_operation_log` 的 `request_params / response_data` text 字段）。
- **不需要重写**：直接依赖注入全局 `@Primary` 的 ObjectMapper（[JacksonConfig.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/config/JacksonConfig.java#L70-L82) 已配置 **Long→String**、日期统一 `yyyy-MM-dd HH:mm:ss`、中国时区、未知字段不报错）。
- **为什么不能 new**：`new ObjectMapper()` 是"裸"实例，会丢失 Long→String、日期格式化等全局配置；且与 HTTP 消息转换器（[JacksonConfig.java#L88](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/config/JacksonConfig.java#L88-L91) 挂到了 `MappingJackson2HttpMessageConverter`）不是同一份，日志格式会和接口返回不一致。
- **补充**：`ObjectMapper` 来自 `jackson-databind`，`spring-boot-starter-web` 已内嵌，无需额外加依赖；只需额外加 try-catch 兜底，防止序列化失败影响主流程。