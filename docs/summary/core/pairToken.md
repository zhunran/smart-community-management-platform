读取双 token 相关核心代码，确保梳理基于真实实现


用户要求梳理项目中双 token 实现的具体结构以及数据流转。这是一个分析任务，需要基于真实代码，不要猜测。

我已经从文档中了解了很多关于双 token 的信息，但为了准确梳理，我应该读取关键代码文件：
1. JwtUtil.java
2. TokenService.java
3. SecurityUtil.java
4. AuthInterceptor.java
5. AdminAuthController.java（已有全文）
6. 可能 OwnerAuthController.java

让我先找到并读取关键文件。这些文件在 property-framework 模块。

路径应该是：
- property-framework/src/main/java/com/property/framework/web/security/JwtUtil.java
- property-framework/src/main/java/com/property/framework/web/security/TokenService.java
- property-framework/src/main/java/com/property/framework/web/security/SecurityUtil.java
- property-framework/src/main/java/com/property/framework/web/interceptor/AuthInterceptor.java

让我并行读取这些文件。也可以用 Glob 先确认路径。

让我先用 Glob 或直接 Read。文档中已经给出了路径：
- JwtUtil.java: file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/JwtUtil.java
- TokenService.java: .../TokenService.java
- SecurityUtil.java: .../SecurityUtil.java
- AuthInterceptor.java: file:///.../property-framework/src/main/java/com/property/framework/web/interceptor/AuthInterceptor.java

让我直接读这些文件。同时读 OwnerAuthController 来对比 owner 端。

为了高效，我并行读取核心文件。

关键文件列表：
1. TokenService.java
2. JwtUtil.java
3. SecurityUtil.java
4. AuthInterceptor.java
5. OwnerAuthController.java（owner 端认证）

让我先 Glob 确认 security 目录和 interceptor 目录内容，避免路径猜错。

实际上文档已经给了准确路径，我直接读。但为了稳妥，我可以并行读 4 个文件 + OwnerAuthController。让我用 Glob 快速确认 SecurityUtil 是否存在。

我直接读这几个文件。先并行读 JwtUtil、TokenService、SecurityUtil、AuthInterceptor。

我已经读取了核心文件。现在梳理双 token 实现的具体结构和数据流转。

我注意到 AuthInterceptor 里有几个问题（可能是我要客观提及的）：
1. 黑名单检查：`if (tokenService!=null && tokenService.isBlacklisted(token))` 只打了 log.warn，没有 throw 拒绝！这是一个疑似 bug —— 黑名单 token 并没有被真正拒绝。
2. `token==null` 时只 log.warn，然后继续 `jwtUtil.getLoginUser(token)`，会抛异常（因为 token 是 null），最终 throw AuthException。逻辑上能走通但依赖 try-catch。
3. Token 提取：COOKIE_NAME = "token"（access token cookie 名一致）。

这些是代码审查发现，但用户的问题核心是"梳理双 token 实现的具体结构和数据流转"，不是让我找 bug。不过我应该忠实呈现，并且可以顺带点出黑名单检查那处"只记日志未拒绝"的现象，因为这直接影响数据流转的准确性。

但要注意：用户要求"梳理结构以及数据流转"，我应该聚焦在清晰的梳理。对于 AuthInterceptor 里的黑名单"只 warn 不 reject"，这是一个客观事实，如果我不指出，梳理的"数据流转"（登出后 access token 是否真正失效）就不准确。我应该如实说明，但可以简短标注。

让我先确认 owner 端是否也有类似实现，以完整覆盖"项目中双 token"。Admin 端我已经有了 AdminAuthController 全文。Owner 端我需要看 OwnerAuthController。但从文档看，结构类似（refreshKeys 前缀不同、Cookie 路径不同）。

为了完整，我应该也读一下 OwnerAuthController。但从项目记忆和文档，核心结构（JwtUtil/TokenService/SecurityUtil/AuthInterceptor）是共享的 framework 层，Admin 和 Owner 只是 Controller 层不同（role 不同、Cookie path 不同、端口不同）。

我可以读 OwnerAuthController 来完整说明。但也许不必，因为核心机制已清楚，Controller 层差异在文档里也说明了。

让我决定：为了准确梳理"项目中"的双 token，我应该简要确认 owner 端 Controller 的差异。但核心流转已经完整（framework 层是共享的）。我读一下 OwnerAuthController 会更严谨。不过为了效率，我可以基于已有信息 + 框架层代码来梳理，owner 端差异只需说明"role=owner、Cookie 名/path 不同"。

实际上，让我快速读 OwnerAuthController 以保证准确。同时这也能确认 owner 端 Cookie 名称。

其实从 AuthInterceptor 的 COOKIE_NAME="token" 来看，access cookie 名是 "token"。Admin 端 ACCESS_COOKIE="token"，REFRESH_COOKIE="refresh_token"。

Owner 端应该类似。让我读 OwnerAuthController 确认，避免猜测。

不过，我认为核心梳理已经足够，不读 owner 也能准确描述框架层的共享流转，且 controller 层差异我在文档里已知（role 不同、refresh cookie path 不同）。为了避免过度，我可以直接基于现有代码梳理，并说明 admin/owner 两端共享 framework 层、仅 Controller 层 role 和 Cookie path 不同。

但为了严谨（用户说"不要猜测"是之前的约定），我还是快速读一下 OwnerAuthController，因为我要描述 owner 端差异。让我读它。

实际上，我已经有足够信息，而且文档（关键实现逻辑.md）里明确写了 owner 端：
- 登录：手机号 + 密码 + 图形验证码
- REFRESH_COOKIE_PATH 不同

为了准确，读一下 OwnerAuthController。这是合理的一步。

让我读 OwnerAuthController.java。

现在我有完整的信息来梳理双 token 实现的结构和数据流转。让我组织一个清晰、准确、基于代码的回答。

结构梳理：

**1. 参与组件（framework 层共享 + 两端 Controller）**

共享（property-framework）：
- `JwtUtil`（@Component，@ConditionalOnProperty("jwt.secret")）：生成/解析/校验 JWT，定义 type claim
- `TokenService`（@Service，@ConditionalOnBean(JwtUtil.class)）：签发/刷新/登出/黑名单，封装 JwtUtil + RedisUtil
- `SecurityUtil`：ThreadLocal 存 LoginUser
- `AuthInterceptor`（HandlerInterceptor）：拦截请求、解析 token、写入/清理 ThreadLocal

业务端（admin-api / owner-api）：
- `AdminAuthController`（role=admin，refresh path=/api/admin/auth/refresh）
- `OwnerAuthController`（role=owner，refresh path=/api/owner/auth/refresh，额外验证码）

基础设施：
- Redis（refresh token 存储 + access token 黑名单）
- Cookie（httpOnly 传输）

**2. Token 结构（claims 差异）**

Access Token：
- subject = userId
- claim type=access
- 额外 claim：username、realName、role
- 短效（jwt.expiration）

Refresh Token：
- subject = userId
- claim type=refresh
- 只有 userId（无 username/realName/role）
- 长效（jwt.refresh-expiration）

**3. 数据流转**

我画几个时序：

登录流程：
1. 前端 POST /login（admin 用户名密码 / owner 手机号+密码+验证码）
2. Controller 校验 → Service 校验用户 → 构建 LoginUser
3. tokenService.issue(loginUser)
   - jwtUtil.generateAccessToken → access（含完整用户信息，type=access）
   - jwtUtil.generateRefreshToken → refresh（仅 userId，type=refresh）
   - redisUtil.set("token:refresh:{role}:{userId}", refreshToken, TTL=refreshExpiration)
4. writeAccessCookie（name=token, path=/, maxAge=accessExpiration）
5. writeRefreshCookie（name=refresh_token, path=/api/{admin|owner}/auth/refresh, maxAge=refreshExpiration）
6. 返回 access token 到响应体（前端也可拿到，但主要靠 cookie）

请求鉴权流程：
1. 请求到达 → AuthInterceptor.preHandle
2. 提取 token：优先 Authorization: Bearer，其次 cookie "token"
3. 黑名单检查（isBlacklisted）
4. jwtUtil.getLoginUser(token) → parseToken 验签 + 校验 type=access → 构建 LoginUser
5. SecurityUtil.setLoginUser(loginUser) 存入 ThreadLocal
6. 业务层通过 SecurityUtil.getUserId() 等获取
7. afterCompletion → SecurityUtil.clear() 清理 ThreadLocal

刷新流程：
1. Access 过期 → 前端 401 → 调 POST /refresh（携带 refresh cookie，path 匹配）
2. @CookieValue(refresh_token)
3. tokenService.refresh(refreshToken, role)
   - isType(refresh) 校验 type=refresh
   - getUserId(refreshToken)
   - 从 Redis 读 token:refresh:{role}:{userId}，比对一致
   - 构建新 LoginUser（只 userId+role，业务信息可补全）
   - generateAccessToken → 新 access
4. writeAccessCookie 更新 access cookie
5. 返回

登出流程：
1. POST /logout（携带 access cookie）
2. 解析 userId（getJwtUtilUserId，失败忽略）
3. tokenService.logout(accessToken, role, userId)
   - 删除 Redis 的 refresh key（token:refresh:{role}:{userId}）
   - access token 加入黑名单 token:blacklist:{accessToken}="1"，TTL=剩余有效期
4. clearCookie 清 access（path=/）和 refresh（path=refresh path）

**4. 关键设计点（与数据流转相关的约束）**
- Refresh Token 只在 path /api/admin/auth/refresh 携带 → 业务请求不会带上 refresh，减少泄露面
- Access Token 通过 cookie path=/ 携带 → 所有业务请求携带
- type claim 区分：getLoginUser 强制 type=access，refresh 强制 type=refresh，防止混用
- Refresh 存 Redis → 服务端可控吊销（删除 key 即失效）
- 黑名单 → 登出后 access 在剩余有效期内也被拒

**5. 需要如实指出的问题（AuthInterceptor 黑名单只是 warn 未 reject）**

我注意到 AuthInterceptor 第 64-67 行：
```java
if (tokenService!=null && tokenService.isBlacklisted(token)) {
    log.warn("Token已失效（黑名单）URI={}",request.getRequestURI());
}
```
这里只是 log.warn，没有 throw 或 return false！意味着黑名单里的 access token 实际上**不会**被拦截器拒绝，而是继续走 jwtUtil.getLoginUser(token) 成功解析（因为 token 签名和过期都有效）并放行。

这是一个真实的实现缺陷，直接影响"登出后的 access token 是否真正失效"的数据流转结论。我应该如实指出，因为它与"黑名单机制"的数据流转相关。

但用户的问题核心是"梳理双 token 实现的具体结构以及数据流转"，我应该：
1. 完整梳理结构 + 流转（主任务）。
2. 在梳理到"黑名单"环节时，如实标注这个"黑名单仅记录日志、未真正拒绝"的现象，作为一个观察点。

这不算跑题，因为它是流转的一部分，且客观存在。我会在结尾单独点出这一处，保持客观、不夸大。

另外还有 `token==null` 时只 warn 然后继续走 try 里 getLoginUser(null) 抛异常，最终 throw AuthException。这个能兜住，但逻辑绕。这个是次要的，我可以简单提或不提。重点是黑名单未拒绝。

现在让我组织输出。用中文，清晰分节，配流程图（用 mermaid 或 ASCII）。用户是开发者，用 markdown 呈现，带 file 链接引用。

我会给出：
1. 组件结构表
2. Token 内容对比
3. 数据流转（登录/鉴权/刷新/登出 四个时序，用 mermaid sequenceDiagram 或 ASCII）
4. Redis key 设计
5. 一个如实指出的缺陷

关于 mermaid，CommonMark 渲染可能支持 mermaid 也可能不支持。为了保险，我用 ASCII 流程图（和之前文档风格一致）或者简洁的文字流转。用 mermaid 更清晰但可能不渲染。我用文字 + 箭头列表，避免依赖 mermaid 渲染。

实际上我可以两者结合，用简洁的列表式流转。让我用分步骤描述 + 关键的图。

长度控制：用户要"梳理"，应该完整但不啰嗦。我会结构化。

让我写。

关于字段：LoginUser 有 token 字段（getLoginUser 里 .token(token)）。这个也值得一提，但非核心。

Redis key：
- refresh: token:refresh:{role}:{userId} = refreshToken，TTL=refreshExpiration
- blacklist: token:blacklist:{accessToken} = "1"，TTL=remaining

Cookie：
- access: name=token, path=/, httpOnly, maxAge=accessExpiration
- refresh: name=refresh_token, path=/api/{admin|owner}/auth/refresh, httpOnly, maxAge=refreshExpiration

好，组织回答。我会用 file 引用。

对于 AuthInterceptor 黑名单缺陷，我用 [AuthInterceptor.java #L64-L67] 引用并说明。

我现在写最终回答。不需要调用 advisor（这是分析梳理任务，已完成足够调研）。

让我写一个结构清晰的回答。
双 token 实现由 **framework 层（共享机制） + 两端 Controller（差异入口） + Redis/Cookie（存储载体）** 三部分组成。完整梳理如下。

## 一、组件结构

| 组件 | 位置 | 职责 |
|------|------|------|
| [JwtUtil](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/JwtUtil.java) | framework | 生成/解析/校验 JWT，定义 `type` claim，提供签名、过期校验 |
| [TokenService](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/TokenService.java) | framework | 签发/刷新/登出/黑名单，封装 JwtUtil + RedisUtil，是 Controller 操作 token 的唯一入口 |
| [SecurityUtil](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/SecurityUtil.java) | framework | ThreadLocal 存 `LoginUser`，供业务层随时取 userId/username/role |
| [AuthInterceptor](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/interceptor/AuthInterceptor.java) | framework | 请求入口拦截：提取 token → 校验 → 写入 ThreadLocal → 请求结束清理 |
| [AdminAuthController](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/java/com/property/adminapi/controller/AdminAuthController.java) | admin-api | role=admin，refresh cookie path=`/api/admin/auth/refresh` |
| [OwnerAuthController](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/java/com/property/ownerapi/controller/OwnerAuthController.java) | owner-api | role=owner，refresh cookie path=`/api/owner/auth/refresh`，多一道图形验证码 |

两端 Controller 结构完全一致，仅 `ROLE` 常量和 `REFRESH_COOKIE_PATH` 不同；核心机制全部下沉到 framework，**不重复实现**。

## 二、Token 内容差异（claim 对比）

| | Access Token | Refresh Token |
|---|-------------|---------------|
| `subject` | userId | userId |
| `type` | `access` | `refresh` |
| 额外 claim | username、realName、role | 无（仅 userId） |
| 有效期 | `jwt.expiration`（短效） | `jwt.refresh-expiration`（长效） |
| 用途 | 业务鉴权 | 仅换取新 access |

关键约束由 [JwtUtil.getLoginUser](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/JwtUtil.java#L95-L107) 强制：`type != access` 直接抛 `JwtException`，**Refresh Token 无法当 Access Token 用**。

## 三、数据流转

### 1. 登录签发

```
前端 POST /login
  → 校验（admin: 用户名+密码 / owner: 手机号+密码+验证码）
  → 构建 LoginUser(userId, username, realName, role)
  → TokenService.issue(loginUser):
       jwtUtil.generateAccessToken()   → access（含完整用户信息）
       jwtUtil.generateRefreshToken()  → refresh（仅 userId）
       redis.set("token:refresh:{role}:{userId}", refreshToken, TTL=refreshExpiration)
  → writeAccessCookie:  name=token,        path=/,                            maxAge=accessExpiration
  → writeRefreshCookie: name=refresh_token, path=/api/{admin|owner}/auth/refresh, maxAge=refreshExpiration
  → 响应体返回 access token
```

### 2. 业务请求鉴权（AuthInterceptor）

```
请求 → preHandle
  → 提取 token：优先 Authorization: Bearer，其次 Cookie 里的 "token"
  → isBlacklisted(token) 检查黑名单
  → jwtUtil.getLoginUser(token)：验签 + 校验 type=access → LoginUser
  → SecurityUtil.setLoginUser(loginUser)   // 存入 ThreadLocal
  → 业务层通过 SecurityUtil.getUserId()/getRole() 获取
  → afterCompletion → SecurityUtil.clear()  // 清理 ThreadLocal 防泄漏
```

### 3. 无感刷新

```
Access 过期 → 前端收到 401 → POST /refresh（自动携带 refresh_token cookie，因 path 匹配）
  → @CookieValue("refresh_token")
  → TokenService.refresh(refreshToken, role):
       ① isType(type=refresh) 校验类型
       ② getUserId(refreshToken)
       ③ redis.get("token:refresh:{role}:{userId}")，与传入值比对（不一致→拒绝）
       ④ 重建 LoginUser（仅 userId+role）→ generateAccessToken() 生成新 access
  → writeAccessCookie 更新 access cookie
```

### 4. 登出

```
POST /logout（携带 access cookie）
  → 解析 userId（getJwtUtilUserId，解析失败被忽略、不影响主流程）
  → TokenService.logout(accessToken, role, userId):
       ① redis.delete("token:refresh:{role}:{userId}")      // 删 refresh，刷新接口即刻失效
       ② redis.set("token:blacklist:{accessToken}", "1", TTL=剩余有效期)  // access 进黑名单
  → clearCookie：清 access（path=/）和 refresh（path=refresh path）
```

## 四、Redis Key 设计

| Key | Value | TTL | 作用 |
|-----|-------|-----|------|
| `token:refresh:{role}:{userId}` | refreshToken | refresh 有效期 | 刷新时比对、登出删除（服务端可控吊销） |
| `token:blacklist:{accessToken}` | `"1"` | access 剩余有效期 | 登出后 access 立即失效 |

## 五、一处需要你留意的实现细节

梳理时发现了 [AuthInterceptor](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/interceptor/AuthInterceptor.java#L64-L67) 的黑名单检查**只记日志、未真正拒绝**：

```java
if (tokenService != null && tokenService.isBlacklisted(token)) {
    log.warn("Token已失效（黑名单）URI={}", request.getRequestURI());
}
```

这里缺少 `throw`/`return false`。也就是说：登出加入黑名单的 access token，在剩余有效期内**仍会被放行**（因为签名和过期都仍有效，`getLoginUser` 能正常解析）。这意味着"登出即失效"目前**没有真正生效**——refresh token 已删（刷新接口确实会失效），但 access token 的黑名单拦截是空转的。

这直接影响你「登出流程」的完整性判断。如果这是有意的（已经靠 Redis 删 refresh 来兜底），建议补个注释说明；如果是遗漏，补一行 `throw new AuthException(...)` 即可让黑名单真正生效。需要我帮你改成真正拒绝吗？