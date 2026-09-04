# 阶段二操作手册：双 Token 认证（Access + Refresh 无感刷新）

> 配套文档：[升级改造计划书](./upgrade-plan.md)、[阶段一操作手册](./phase1-upgrade-manual.md)
> 编制日期：2026-08-17
> 阶段范围：单 Token → 双 Token（Access Token 2h + Refresh Token 7d）、Access Token 黑名单、无感刷新
> 前置条件：**阶段一已完成**（JDK 25 + Spring Boot 4.1.0 + Redis 基础设施已就绪，`RedisUtil` 可用）
> 影响范围：`property-framework`（JwtUtil / AuthInterceptor）、`property-admin-api` 与 `property-owner-api`（登录/登出/刷新接口）、前端 admin-web / owner-web（401 自动刷新重试）

---

## 目录

- [1. 变更总览](#1-变更总览)
- [2. 设计说明](#2-设计说明)
- [3. 后端改造（步骤 1-9）](#3-后端改造步骤-1-9)
- [4. 前端改造（步骤 10-11）](#4-前端改造步骤-10-11)
- [5. 编译与验证](#5-编译与验证)
- [6. 验收标准](#6-验收标准)
- [附录 A：完整文件变更清单](#附录-a完整文件变更清单)
- [附录 B：Redis Key 规划（阶段二）](#附录-bredis-key-规划阶段二)
- [附录 C：认证流程时序](#附录-c认证流程时序)

---

## 1. 变更总览

### 1.1 认证机制变更矩阵

| 维度 | 阶段一（现状） | 阶段二（目标） |
|------|--------------|--------------|
| Token 数量 | 单 Token | **双 Token**（Access + Refresh） |
| Access Token 有效期 | 24h（`JWT_EXPIRATION=86400000`） | **2h**（短效，降低泄露风险） |
| Refresh Token | 无 | **7d**，服务端 Redis 存储 |
| 登录态载体 | Access Token 写 httpOnly Cookie | Access + Refresh 均写 httpOnly Cookie；Refresh 另存 Redis 校验 |
| Token 吊销能力 | 无（JWT 无状态不可吊销） | **黑名单**（登出/刷新时 Access 入黑名单） |
| 过期处理 | 直接跳登录页 | **无感刷新**（401 自动调 /refresh 重试一次） |
| 登出 | 仅清 Cookie（Token 仍有效） | 删 Refresh Token + Access 入黑名单（真正失效） |

### 1.2 变更文件清单

| 操作 | 文件路径 |
|------|---------|
| 修改 | [JwtUtil.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/JwtUtil.java)（扩展双 Token 生成/类型区分） |
| 修改 | [AuthInterceptor.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/interceptor/AuthInterceptor.java)（黑名单检查 + 仅接受 Access 类型） |
| 新增 | `property-framework/.../web/security/TokenService.java`（双 Token 签发/刷新/登出统一封装） |
| 修改 | [AdminAuthController.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/java/com/property/adminapi/controller/AdminAuthController.java)（登录写双 Cookie、新增 /refresh、登出吊销） |
| 修改 | [OwnerAuthController.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/java/com/property/ownerapi/controller/OwnerAuthController.java)（同上） |
| 修改 | [property-admin-api/.../application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/resources/application.yml)（新增 refresh-expiration） |
| 修改 | [property-owner-api/.../application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/resources/application.yml)（新增 refresh-expiration） |
| 修改 | [docker/.env](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/.env)（调整 JWT_EXPIRATION、新增 JWT_REFRESH_EXPIRATION） |
| 修改 | [admin-web request.ts](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-web/src/utils/request.ts)、[auth.ts](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-web/src/api/auth.ts)（401 自动刷新重试） |
| 修改 | [owner-web request.ts](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-web/src/utils/request.ts)、[auth.ts](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-web/src/api/auth.ts)（401 自动刷新重试） |

---

## 2. 设计说明

### 2.1 Token 设计

| Token | 有效期 | 存储位置 | 用途 | claim `type` |
|-------|--------|---------|------|-------------|
| Access Token | 2 小时 | 前端 httpOnly Cookie（name=`token`） | 每次请求鉴权 | `access` |
| Refresh Token | 7 天 | 前端 httpOnly Cookie（name=`refresh_token`）+ 服务端 Redis | 刷新 Access Token | `refresh` |

**关键点**：
- 两种 Token 都由 JWT 签名，通过 claim `type` 区分（`access` / `refresh`）。
- `AuthInterceptor` 只接受 `type=access` 的 Token，防止 Refresh Token 被直接用于业务请求。
- Refresh Token 除了签发给前端外，**同时以 `token:refresh:{role}:{userId}` 为 Key 存入 Redis**。刷新时需比对 Redis 中的值，实现服务端可控吊销（登出即删除，一处登出全端失效）。
- 登出时：删除 Redis 中的 Refresh Token + 将当前 Access Token 写入黑名单 `token:blacklist:{token}`（TTL = Access 剩余有效期），使其在过期前无法再用。

### 2.2 为什么 Refresh Token 也用 httpOnly Cookie

现有前端登录态完全依赖 httpOnly Cookie（`withCredentials: true`），前端 JS 读不到 Token。为保持这一安全模型且最小化前端改动：
- Access Token 继续写入 Cookie `token`，路径 `/`。
- Refresh Token 写入 Cookie `refresh_token`，**路径限定为刷新接口路径**（`/api/admin/auth/refresh` 或 `/api/owner/auth/refresh`），使其只在调用刷新接口时被浏览器自动携带，缩小暴露面。
- 前端 401 时无需读取 Token，直接 `POST /refresh`（浏览器自动带上 refresh Cookie），后端校验通过后重新下发 Access Cookie。

### 2.3 管理端 / 业主端隔离

管理端与业主端使用**不同 JWT 密钥**（`JWT_ADMIN_SECRET` / `JWT_OWNER_SECRET`），`JwtUtil` 是按各自 `jwt.secret` 注入的独立 Bean，天然隔离。Redis Key 中加入 `role` 段（`admin` / `owner`）进一步避免 userId 冲突（管理员与业主 ID 空间独立）。

---

## 3. 后端改造（步骤 1-9）

### 步骤 1：环境变量调整（.env）

**文件**：[docker/.env](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/.env)

将 Access Token 有效期缩短为 2 小时，并新增 Refresh Token 有效期（7 天）：

```env
# JWT（阶段二：双 Token）
JWT_ADMIN_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5YWJjZGVmZ2hpamtsbW5vcA
JWT_OWNER_SECRET=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5YWJjZGVmZ2hpamtsbW5vcA
JWT_EXPIRATION=7200000            # Access Token：2 小时（原 86400000 24h）
JWT_REFRESH_EXPIRATION=604800000  # Refresh Token：7 天
```

> 本地开发（IDEA 运行配置）同样需要新增 `JWT_REFRESH_EXPIRATION` 环境变量，值 `604800000`。

### 步骤 2：application.yml 新增 refresh 有效期

**文件**：[property-admin-api/.../application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/resources/application.yml) 与 [property-owner-api/.../application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/resources/application.yml)

在两个文件的 `jwt:` 节点下新增 `refresh-expiration`：

```yaml
# JWT 配置（管理端 / 业主端）
jwt:
  secret: ${JWT_ADMIN_SECRET}          # owner 端为 ${JWT_OWNER_SECRET}
  expiration: ${JWT_EXPIRATION}        # Access Token 有效期
  refresh-expiration: ${JWT_REFRESH_EXPIRATION}  # 新增：Refresh Token 有效期
```

### 步骤 3：扩展 JwtUtil 支持双 Token

**文件**：[JwtUtil.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/JwtUtil.java)

改造要点：注入 `refresh-expiration`；新增 `generateAccessToken` / `generateRefreshToken`（写入 `type` claim）；`getLoginUser` 校验 Token 类型必须为 `access`；新增取 `type`、`userId`、剩余有效期的辅助方法。完整替换为：

```java
package com.property.framework.web.security;

import com.property.common.dto.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类（阶段二：双 Token）
 * - Access Token（type=access，短效）用于业务请求鉴权
 * - Refresh Token（type=refresh，长效）用于刷新 Access Token
 */
@Component
@ConditionalOnProperty(name = "jwt.secret")
public class JwtUtil {

    /** Token 类型 claim 名 */
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long expiration;         // Access Token 有效期（毫秒）
    private final long refreshExpiration;  // Refresh Token 有效期（毫秒）

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration,
                   @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    public long getAccessExpiration() {
        return expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    /**
     * 生成 Access Token（含用户信息，type=access）
     */
    public String generateAccessToken(LoginUser loginUser) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim("username", loginUser.getUsername())
                .claim("realName", loginUser.getRealName())
                .claim("role", loginUser.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 Refresh Token（仅含 userId，type=refresh）
     */
    public String generateRefreshToken(LoginUser loginUser) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token → Claims（签名/过期校验失败会抛异常）
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 解析 Access Token → LoginUser
     * 若 Token 类型不是 access，抛 JwtException（防止 Refresh Token 用于业务请求）
     */
    public LoginUser getLoginUser(String token) {
        Claims claims = parseToken(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("非法的 Token 类型，要求 access");
        }
        return LoginUser.builder()
                .userId(Long.valueOf(claims.getSubject()))
                .username(claims.get("username", String.class))
                .realName(claims.get("realName", String.class))
                .role(claims.get("role", String.class))
                .token(token)
                .build();
    }

    /**
     * 从任意 Token 中提取 userId（不校验类型）
     */
    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    /**
     * 判断 Token 是否为指定类型
     */
    public boolean isType(String token, String type) {
        try {
            return type.equals(parseToken(token).get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取 Token 剩余有效期（毫秒），已过期返回 0
     */
    public long getRemainingMillis(String token) {
        Date exp = parseToken(token).getExpiration();
        long remaining = exp.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    /**
     * 校验 Token 是否有效（签名 + 未过期）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

> **兼容性**：原 `generateToken(LoginUser)` 方法被 `generateAccessToken` 取代。步骤 6 会同步修改两处调用点（`SysUserServiceImpl` / `OwnerAuthServiceImpl`）。若希望平滑过渡，可暂时保留 `generateToken` 作为 `generateAccessToken` 的别名，但本手册建议直接替换以保持代码整洁。

### 步骤 4：新增 TokenService（双 Token 统一封装）

**新建文件**：`property-framework/src/main/java/com/property/framework/web/security/TokenService.java`

将「签发双 Token / 刷新 / 登出吊销 / 黑名单校验」的逻辑集中到一个可复用的服务，供 admin 与 owner 两端 Controller 调用，避免重复代码。

```java
package com.property.framework.web.security;

import com.property.common.dto.LoginUser;
import com.property.framework.util.RedisUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 双 Token 服务（阶段二）
 * 负责：签发 Access+Refresh、刷新 Access、登出吊销、黑名单校验
 */
@Service
@RequiredArgsConstructor
@ConditionalOnBean(JwtUtil.class)
public class TokenService {

    private static final String REFRESH_KEY_PREFIX = "token:refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    /** 双 Token 结果 */
    @Getter
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    /**
     * 登录成功后签发双 Token，并把 Refresh Token 存入 Redis
     */
    public TokenPair issue(LoginUser loginUser) {
        String accessToken = jwtUtil.generateAccessToken(loginUser);
        String refreshToken = jwtUtil.generateRefreshToken(loginUser);

        String refreshKey = refreshKey(loginUser.getRole(), loginUser.getUserId());
        redisUtil.set(refreshKey, refreshToken, Duration.ofMillis(jwtUtil.getRefreshExpiration()));

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 用 Refresh Token 刷新 Access Token
     * 校验：签名有效 + type=refresh + 与 Redis 中存储的一致
     *
     * @return 新的 Access Token；校验失败返回 null
     */
    public String refresh(String refreshToken, String role) {
        if (refreshToken == null || !jwtUtil.isType(refreshToken, JwtUtil.TYPE_REFRESH)) {
            return null;
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        String refreshKey = refreshKey(role, userId);
        Object stored = redisUtil.get(refreshKey);
        // Redis 中不存在（已登出/已过期）或与传入的不一致 → 拒绝
        if (stored == null || !refreshToken.equals(stored.toString())) {
            return null;
        }
        // 重新构造 LoginUser（Refresh Token 仅含 userId，业务信息在此处可按需补全）
        LoginUser loginUser = LoginUser.builder()
                .userId(userId)
                .role(role)
                .build();
        return jwtUtil.generateAccessToken(loginUser);
    }

    /**
     * 刷新时若需要保留 username/realName，可用带业务信息的重载
     */
    public String refresh(String refreshToken, LoginUser loginUser) {
        String newAccess = refresh(refreshToken, loginUser.getRole());
        return newAccess == null ? null : jwtUtil.generateAccessToken(loginUser);
    }

    /**
     * 登出：删除 Refresh Token + Access Token 加入黑名单
     */
    public void logout(String accessToken, String role, Long userId) {
        // 1. 删除 Redis 中的 Refresh Token
        if (userId != null) {
            redisUtil.delete(refreshKey(role, userId));
        }
        // 2. Access Token 加入黑名单，TTL = 剩余有效期
        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            long remaining = jwtUtil.getRemainingMillis(accessToken);
            if (remaining > 0) {
                redisUtil.set(blacklistKey(accessToken), "1", Duration.ofMillis(remaining));
            }
        }
    }

    /**
     * 判断 Access Token 是否在黑名单中
     */
    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisUtil.hasKey(blacklistKey(accessToken)));
    }

    private String refreshKey(String role, Long userId) {
        return REFRESH_KEY_PREFIX + role + ":" + userId;
    }

    private String blacklistKey(String accessToken) {
        return BLACKLIST_KEY_PREFIX + accessToken;
    }
}
```

> **说明**：`refresh(String, String)` 生成的新 Access Token 仅含 userId 和 role（username/realName 为空）。若下游接口强依赖 `SecurityUtil.getUsername()`，请在各端刷新接口中先按 userId 查库补全用户信息再签发（见步骤 7 的可选增强）。对本项目大多数接口而言，鉴权只需 userId + role，简化版已够用。

### 步骤 5：AuthInterceptor 增加黑名单检查

**文件**：[AuthInterceptor.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/interceptor/AuthInterceptor.java)

在 `preHandle` 中，解析 Token 之前先检查黑名单；注入 `TokenService`（沿用现有延迟注入模式）。改动片段：

```java
    private JwtUtil jwtUtil;
    private TokenService tokenService;   // 新增

    public void setJwtUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** 新增：注入 TokenService（与 JwtUtil 同样在配置类中延迟注入） */
    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }
```

```java
        // 解析 Token
        String token = extractToken(request);
        if (token == null) {
            log.warn("未提供Token, URI={}", request.getRequestURI());
            throw new AuthException("未提供认证Token");
        }

        // 新增：黑名单检查（已登出的 Access Token 直接拒绝）
        if (tokenService != null && tokenService.isBlacklisted(token)) {
            log.warn("Token已失效(黑名单), URI={}", request.getRequestURI());
            throw new AuthException("登录已失效，请重新登录");
        }

        try {
            // getLoginUser 内部会校验 type=access
            LoginUser loginUser = jwtUtil.getLoginUser(token);
            SecurityUtil.setLoginUser(loginUser);
        } catch (Exception e) {
            log.warn("Token解析失败, URI={}, error={}", request.getRequestURI(), e.getMessage());
            throw new AuthException("Token无效或已过期");
        }
```

> **注意**：`AuthInterceptor` 的 `jwtUtil` 是通过配置类（`WebMvcConfig` 或注册拦截器处）手动 `setJwtUtil` 注入的。需在同一处补一行 `interceptor.setTokenService(tokenService)`。搜索现有 `setJwtUtil(` 的调用点，在其旁边添加即可。

### 步骤 6：登录服务返回 LoginUser（供 Controller 签发双 Token）

**文件**：[SysUserServiceImpl.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/java/com/property/adminapi/service/impl/SysUserServiceImpl.java) 与 [OwnerAuthServiceImpl.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/java/com/property/ownerapi/service/impl/OwnerAuthServiceImpl.java)

双 Token 的签发涉及写 Cookie（需 `HttpServletResponse`），放在 Controller 层更合适。因此 Service 层不再自己生成 Token，改为将 `LoginUser` 返回给 Controller。

**方案（推荐，改动小）**：Service 仍返回 `LoginResponse`，但**不再调用 `jwtUtil.generateToken`**，`token` 字段留空；同时 Service 内部构建好的 `LoginUser` 通过 `LoginResponse` 透出（或 Controller 用响应中的 userId/username/realName/role 重建 LoginUser）。

以 `SysUserServiceImpl` 为例，删除第 6 步的 Token 生成，直接返回：

```java
        // 5. 构建 LoginUser（保留，供 Controller 签发 Token）
        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(getRoleByUserType(user.getUserType()))
                .build();

        log.info("登录成功 [userId={}, username={}]", user.getId(), user.getUsername());

        // 6. 返回响应（token 由 Controller 签发，此处不再生成）
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(getRoleByUserType(user.getUserType()))
                .userType(user.getUserType())
                .build();
```

移除 `SysUserServiceImpl` 中不再使用的 `JwtUtil jwtUtil` 依赖。`OwnerAuthServiceImpl` 做相同处理（保留更新最后登录时间的逻辑，仅移除 `jwtUtil.generateToken`）。

> **更简做法**：若不想改 Service 签名，可让 Service 继续注入 `TokenService` 并在其中签发双 Token，然后把 Access Token 通过返回值带回 Controller 写 Cookie、Refresh Token 由 TokenService 存 Redis 并另行返回。但由于 Refresh Token 也要写 Cookie，仍需 `HttpServletResponse`，所以本手册采用「Service 只做认证、Controller 负责签发与落 Cookie」的分层。

### 步骤 7：改造登录/登出接口 + 新增刷新接口

**文件**：[AdminAuthController.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/java/com/property/adminapi/controller/AdminAuthController.java)

完整替换为（引入 `TokenService`，写 Access + Refresh 双 Cookie，新增 `/refresh`，登出吊销）：

```java
package com.property.adminapi.controller;

import com.property.adminapi.dto.request.LoginRequest;
import com.property.adminapi.dto.response.LoginResponse;
import com.property.adminapi.service.SysUserService;
import com.property.common.dto.LoginUser;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.SkipAuth;
import com.property.framework.web.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端：登录/登出/刷新（阶段二双 Token）
 */
@Tag(name = "管理员认证", description = "管理员登录接口")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@SkipAuth
public class AdminAuthController {

    private static final String ACCESS_COOKIE = "token";
    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/admin/auth/refresh";
    private static final String ROLE = "admin";

    private final SysUserService sysUserService;
    private final TokenService tokenService;

    @Operation(summary = "管理员登录", description = "校验用户名密码，签发 Access+Refresh 双 Token 并写入 httpOnly Cookie")
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                          HttpServletResponse response) {
        LoginResponse loginResponse = sysUserService.login(request);

        LoginUser loginUser = LoginUser.builder()
                .userId(loginResponse.getUserId())
                .username(loginResponse.getUsername())
                .realName(loginResponse.getRealName())
                .role(loginResponse.getRole())
                .build();

        TokenService.TokenPair pair = tokenService.issue(loginUser);
        writeAccessCookie(response, pair.getAccessToken());
        writeRefreshCookie(response, pair.getRefreshToken());

        loginResponse.setToken(pair.getAccessToken());
        return ApiResult.success("登录成功", loginResponse);
    }

    @Operation(summary = "刷新 Access Token", description = "使用 Refresh Token 换取新的 Access Token（无感刷新）")
    @PostMapping("/refresh")
    public ApiResult<Void> refresh(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                                   HttpServletResponse response) {
        String newAccess = tokenService.refresh(refreshToken, ROLE);
        if (newAccess == null) {
            return ApiResult.fail(401, "刷新失败，请重新登录");
        }
        writeAccessCookie(response, newAccess);
        return ApiResult.success("刷新成功", null);
    }

    @Operation(summary = "管理员登出", description = "删除 Refresh Token 并将 Access Token 加入黑名单")
    @PostMapping("/logout")
    public ApiResult<Void> logout(@CookieValue(value = ACCESS_COOKIE, required = false) String accessToken,
                                  HttpServletResponse response) {
        Long userId = null;
        try {
            if (accessToken != null) {
                userId = tokenService.getJwtUtilUserId(accessToken);
            }
        } catch (Exception ignored) {
        }
        tokenService.logout(accessToken, ROLE, userId);
        clearCookie(response, ACCESS_COOKIE, "/");
        clearCookie(response, REFRESH_COOKIE, REFRESH_COOKIE_PATH);
        return ApiResult.success("登出成功", null);
    }

    private void writeAccessCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(ACCESS_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (tokenService.getAccessMaxAgeSeconds()));
        response.addCookie(cookie);
    }

    private void writeRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setPath(REFRESH_COOKIE_PATH);
        cookie.setMaxAge((int) (tokenService.getRefreshMaxAgeSeconds()));
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setPath(path);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
```

为支持上面 Controller 用到的辅助方法，在 `TokenService` 中补充三个便捷方法：

```java
    /** Access Token Cookie 的 maxAge（秒） */
    public int getAccessMaxAgeSeconds() {
        return (int) (jwtUtil.getAccessExpiration() / 1000);
    }

    /** Refresh Token Cookie 的 maxAge（秒） */
    public int getRefreshMaxAgeSeconds() {
        return (int) (jwtUtil.getRefreshExpiration() / 1000);
    }

    /** 从 Access Token 中取 userId（登出时定位 Refresh Key 用） */
    public Long getJwtUtilUserId(String accessToken) {
        return jwtUtil.getUserId(accessToken);
    }
```

**业主端** [OwnerAuthController.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/java/com/property/ownerapi/controller/OwnerAuthController.java) 做**相同改造**，差异仅在：
- 常量 `ROLE = "owner"`，`REFRESH_COOKIE_PATH = "/api/owner/auth/refresh"`；
- `login` 保留原有**图形验证码校验**（`verifyCaptcha`）与 `captcha` 接口不变，只是在业务登录成功后改用 `tokenService.issue(...)` 签发双 Token；
- `login` 使用 `OwnerLoginResponse`（字段为 `ownerId` / `ownerName` / `phone`），构建 `LoginUser` 时 `userId=ownerId`、`username=phone`、`realName=ownerName`、`role="owner"`。

> **可选增强（保留 username/realName）**：若刷新后仍需完整用户信息，可在 `/refresh` 接口中先 `jwtUtil.getUserId` 查库补全，再调用 `tokenService.refresh(refreshToken, fullLoginUser)` 重载。默认简化版对本项目鉴权（仅需 userId + role）已足够。

### 步骤 8：拦截器注册处补充 TokenService 注入

搜索项目中调用 `setJwtUtil(` 的位置（通常在 framework 的 Web 配置类，如 `WebMvcConfig` / `InterceptorConfig`），在其旁补一行：

```java
authInterceptor.setJwtUtil(jwtUtil);
authInterceptor.setTokenService(tokenService);   // 新增
```

该配置类需注入 `TokenService`（构造注入或 `@Autowired`）。若拦截器配置类此前只依赖 `JwtUtil`，一并加入 `TokenService`。

### 步骤 9：登录响应 DTO 的 token 字段含义调整

`LoginResponse.token` / `OwnerLoginResponse.token` 现在承载 **Access Token**（登录态实际以 httpOnly Cookie 为准，返回值仅供调试/兼容）。DTO 无需改字段，仅语义变化。前端仍不直接使用该字段（依赖 Cookie）。

---

## 4. 前端改造（步骤 10-11）

### 步骤 10：新增刷新接口调用

**文件**：[admin-web auth.ts](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-web/src/api/auth.ts)（owner-web 同理，路径换成 `/api/owner/auth/refresh`）

追加刷新接口：

```typescript
export function refreshApi() {
  return request.post('/api/admin/auth/refresh')
}
```

> Refresh Token 存于 httpOnly Cookie（路径限定为 `/refresh`），调用该接口时浏览器自动携带，前端无需手动传参。

### 步骤 11：request.ts 响应拦截器实现 401 无感刷新

**文件**：[admin-web request.ts](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-web/src/utils/request.ts)（owner-web 结构相同，仅刷新路径与清理的 localStorage 键不同）

核心逻辑：401 时先尝试调用 `/refresh`，成功则用原请求配置重试一次；刷新失败或重试仍 401 才跳登录页。需处理并发请求同时 401 的场景（用单一刷新 Promise 去重）。

```typescript
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
  withCredentials: true,
})

// 刷新去重：并发 401 时只发起一次刷新
let refreshing = false
let waiters: Array<(ok: boolean) => void> = []

function onRefreshed(ok: boolean) {
  waiters.forEach((cb) => cb(ok))
  waiters = []
}

function doRefresh(): Promise<boolean> {
  if (refreshing) {
    return new Promise((resolve) => waiters.push(resolve))
  }
  refreshing = true
  return axios
    .post('/api/admin/auth/refresh', null, { withCredentials: true })
    .then((resp) => {
      const ok = resp.data?.code === 200
      onRefreshed(ok)
      return ok
    })
    .catch(() => {
      onRefreshed(false)
      return false
    })
    .finally(() => {
      refreshing = false
    })
}

function redirectLogin() {
  ElMessage.error('登录已过期，请重新登录')
  localStorage.removeItem('username')
  localStorage.removeItem('realName')
  window.location.href = '/login'
}

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  async (error) => {
    const { response, config } = error
    if (response && response.status === 401) {
      // 刷新接口本身返回 401 → 直接跳登录
      if (config.url?.includes('/auth/refresh')) {
        redirectLogin()
        return Promise.reject(error)
      }
      // 已重试过一次仍 401 → 跳登录
      if (config._retry) {
        redirectLogin()
        return Promise.reject(error)
      }
      config._retry = true
      const ok = await doRefresh()
      if (ok) {
        return request(config) // 用新 Access Cookie 重试原请求
      }
      redirectLogin()
      return Promise.reject(error)
    }

    if (response) {
      const status = response.status
      switch (status) {
        case 403:
          ElMessage.error('无权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器异常')
          break
        default:
          ElMessage.error(`请求失败(${status})`)
      }
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  },
)

export default request
```

**owner-web** 的 `request.ts` 做等价改造，区别：
- 刷新路径 `/api/owner/auth/refresh`；
- `redirectLogin` 清理 `owner_id` / `owner_name` / `owner_phone`；
- 无 blob/arraybuffer 分支（按现状保留）。

> **后端配合**：`/refresh` 校验失败时返回业务体 `code=401`（见步骤 7），前端据此判断刷新成败。若你的全局响应封装对 HTTP 200 + body code=401 的处理不同，请确保刷新失败能被 `doRefresh()` 判为 false。

---

## 5. 编译与验证

### 5.1 后端编译

```powershell
$env:JAVA_HOME="D:\jdk25"; mvn -s D:\maven-repo\settings.xml clean compile -DskipTests -q
```

预期 exit 0。若报 `jwt.refresh-expiration` 占位符解析失败，检查步骤 1（.env / IDEA 环境变量）与步骤 2（application.yml）是否都已配置。

### 5.2 前端构建

```powershell
cd property-admin-web; npm run build
cd ../property-owner-web; npm run build
```

### 5.3 功能验证

**登录签发双 Token**：

```powershell
# 登录，观察 Set-Cookie 应同时包含 token 和 refresh_token
curl -i -X POST http://localhost:8081/api/admin/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"******"}'

# Redis 中应出现 Refresh Token
docker exec -it pms-redis redis-cli -a property_redis_2026 keys "token:refresh:*"
# 预期：token:refresh:admin:{userId}
```

**无感刷新**：

```powershell
# 用登录拿到的 cookie 调刷新接口
curl -i -X POST http://localhost:8081/api/admin/auth/refresh `
  -b "refresh_token=<refresh_token_value>"
# 预期：返回 code=200，Set-Cookie 下发新的 token
```

**登出吊销**：

```powershell
curl -i -X POST http://localhost:8081/api/admin/auth/logout `
  -b "token=<access_token>"

# Redis 中 Refresh Token 应被删除，黑名单出现该 Access Token
docker exec -it pms-redis redis-cli -a property_redis_2026 keys "token:blacklist:*"
# 用已登出的 token 访问业务接口应返回 401
```

**前端无感刷新**：浏览器登录后，等待 Access Token 过期（或手动删除 `token` Cookie 保留 `refresh_token`），发起任意业务请求，DevTools Network 中应看到：原请求 401 → 自动 `POST /refresh`（200，下发新 token）→ 原请求自动重试成功，页面无跳登录、无感知。

---

## 6. 验收标准

### 6.1 后端

- [ ] `mvn clean compile -DskipTests` 编译通过
- [ ] 登录接口响应头 `Set-Cookie` 同时包含 `token`（path=/）与 `refresh_token`（path=/api/{admin|owner}/auth/refresh）
- [ ] 登录后 Redis 出现 `token:refresh:{role}:{userId}`，TTL ≈ 7 天
- [ ] Access Token 的 JWT payload 含 `type=access`；Refresh Token 含 `type=refresh`
- [ ] 用 Refresh Token 直接访问业务接口被拒（`type` 校验生效，返回 401）
- [ ] `/refresh` 用有效 Refresh Token 返回 200 并下发新 Access Cookie
- [ ] `/refresh` 用已登出/伪造/过期的 Refresh Token 返回 code=401
- [ ] 登出后：Redis 中 Refresh Token 被删除，`token:blacklist:{token}` 出现且 TTL = Access 剩余有效期
- [ ] 已登出的 Access Token 访问业务接口返回 401（黑名单生效）

### 6.2 前端

- [ ] admin-web / owner-web 构建通过
- [ ] Access Token 过期后，业务请求触发一次自动 `/refresh` 并重试成功，用户无感
- [ ] 并发多个请求同时 401 时只发起一次 `/refresh`（去重生效）
- [ ] Refresh Token 也失效时，正确跳转登录页且不进入死循环
- [ ] 登出后再访问受保护页面被拦截至登录页

### 6.3 回归

- [ ] 管理端登录/登出正常
- [ ] 业主端登录（含图形验证码）/登出正常
- [ ] 各业务接口（账单/缴费/车位/报表）鉴权正常（`SecurityUtil.getUserId()` / `getRole()` 可用）
- [ ] 支付回调、XXL-Job 等 `@SkipAuth` 或无鉴权链路不受影响

---

## 附录 A：完整文件变更清单

### 修改文件

| # | 文件 | 改动摘要 |
|---|------|---------|
| 1 | `property-framework/.../web/security/JwtUtil.java` | 注入 refresh-expiration；新增 generateAccessToken/generateRefreshToken（type claim）；getLoginUser 校验 type=access；新增 getUserId/isType/getRemainingMillis |
| 2 | `property-framework/.../web/interceptor/AuthInterceptor.java` | 注入 TokenService；preHandle 增加黑名单检查 |
| 3 | framework 拦截器注册配置类 | 补 `setTokenService(tokenService)` |
| 4 | `property-admin-api/.../controller/AdminAuthController.java` | 登录签发双 Token 写双 Cookie；新增 /refresh；登出吊销 |
| 5 | `property-owner-api/.../controller/OwnerAuthController.java` | 同上 + 保留图形验证码 |
| 6 | `property-admin-api/.../service/impl/SysUserServiceImpl.java` | 移除 Token 生成，返回 LoginResponse 供 Controller 签发 |
| 7 | `property-owner-api/.../service/impl/OwnerAuthServiceImpl.java` | 同上（保留更新最后登录时间） |
| 8 | `property-admin-api/.../application.yml` | 新增 jwt.refresh-expiration |
| 9 | `property-owner-api/.../application.yml` | 新增 jwt.refresh-expiration |
| 10 | `docker/.env` | JWT_EXPIRATION→7200000；新增 JWT_REFRESH_EXPIRATION=604800000 |
| 11 | `property-admin-web/src/utils/request.ts` | 401 无感刷新重试 + 去重 |
| 12 | `property-admin-web/src/api/auth.ts` | 新增 refreshApi |
| 13 | `property-owner-web/src/utils/request.ts` | 401 无感刷新重试 + 去重 |
| 14 | `property-owner-web/src/api/auth.ts` | 新增 refreshApi |

### 新增文件

| # | 文件 | 用途 |
|---|------|------|
| 1 | `property-framework/.../web/security/TokenService.java` | 双 Token 签发/刷新/登出吊销/黑名单校验统一封装 |

### 删除文件

无。

---

## 附录 B：Redis Key 规划（阶段二）

在阶段一附录 B 基础上，本阶段正式启用 Token 相关 Key：

| Key 格式 | 类型 | TTL | 用途 | 本阶段是否使用 |
|---------|------|-----|------|--------------|
| `sysConfig::{configKey}` | String(JSON) | 30 分钟 | 系统配置缓存（阶段一） | ✅ |
| `lock:payment:callback:{paymentNo}` | String | 30 秒 | 支付回调分布式锁（阶段一） | ✅ |
| `token:refresh:{role}:{userId}` | String | 7 天 | Refresh Token 存储（校验刷新合法性） | ✅ **启用** |
| `token:blacklist:{accessToken}` | String | = Access 剩余有效期 | 已登出 Access Token 黑名单 | ✅ **启用** |

> 相比阶段一预留的 `token:refresh:{userId}`，阶段二在 Key 中加入 `role` 段（`admin`/`owner`），避免管理员与业主的 userId 空间冲突。

**命名规范**：延续阶段一——小写 + 冒号分层；Token 类以 `token:` 前缀；锁类以 `lock:` 前缀；缓存类由 Spring Cache 管理（`{cacheName}::{key}`）。

---

## 附录 C：认证流程时序

```
【登录】
前端 POST /login
  → Service 校验用户名/密码（+ 业主端图形验证码）
  → Controller: TokenService.issue(loginUser)
      · 生成 Access(type=access, 2h) + Refresh(type=refresh, 7d)
      · Refresh 存 Redis: token:refresh:{role}:{userId} (TTL 7d)
  → Set-Cookie: token(path=/) + refresh_token(path=/.../refresh)

【业务请求】
前端请求（自动带 token Cookie）
  → AuthInterceptor.preHandle
      · 黑名单检查 isBlacklisted(token) → 命中则 401
      · getLoginUser(token) 校验 type=access → SecurityUtil 存入 ThreadLocal
  → 放行

【Access 过期（无感刷新）】
业务请求返回 401
  → 前端拦截器: POST /refresh（自动带 refresh_token Cookie）
      → TokenService.refresh(refreshToken, role)
          · 校验签名 + type=refresh + 与 Redis 存储一致
          · 生成新 Access → Set-Cookie: token
  → 前端用新 token 自动重试原请求（用户无感）
  → 若刷新失败(code=401) → 跳登录页

【登出】
前端 POST /logout（带 token Cookie）
  → TokenService.logout(accessToken, role, userId)
      · 删除 Redis token:refresh:{role}:{userId}
      · Access 加入黑名单 token:blacklist:{token} (TTL=剩余有效期)
  → 清除 token 与 refresh_token Cookie
```

