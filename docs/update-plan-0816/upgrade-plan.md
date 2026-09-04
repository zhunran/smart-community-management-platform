# 物业系统 → AI 智慧社区服务平台 升级改造计划书

> 编制日期：2026-08-14
> 项目新名称（暂定）：**AI 智慧社区服务平台**（英文工程名：`smart-community-platform`）
> 状态：待审阅，审阅通过后按阶段实施

---

## 1. 改造背景与目标

### 1.1 背景

当前项目为「物业管理收费系统」，业务已完整实现（楼栋/房屋/业主/账单/缴费/车位/报表），但技术栈停留在传统 CRUD 水平。为提升项目技术含金量与竞争力，参照「AI 智能养老社区管理系统」的技术栈，对现有项目进行技术升级与功能扩展。

### 1.2 改造目标

在不推翻现有业务的前提下，补齐以下技术能力，使项目技术栈达到养老系统同等乃至更高水平：

1. **Spring 版本升级**：JDK 21 → 25、Spring Boot 3.2 → 4.1.0（对齐养老系统）
2. **引入 Redis**：缓存 + Token 黑名单 + 分布式锁
3. **双 Token 认证**：Access Token + Refresh Token，无感刷新
4. **Spring AI + DeepSeek**：AI 物业客服，SSE 流式对话
5. **多端实现**：业主端移动化（Vant），管理端保持 PC 端

### 1.3 明确不做（短信）

| 项 | 决策 | 原因 |
|----|------|------|
| 阿里云短信 | ❌ 不做 | 需企业实名认证 + 费用，个人/学生获取成本高、性价比低 |

**替代方案**：图形验证码（已有）+ 邮件通知（已有），足以满足登录安全和消息通知需求。

---

## 2. 改造范围总览

| 技术点 | 现状 | 目标 | 是否改造 |
|--------|------|------|---------|
| JDK | 21 | 25 | ✅ 升级 |
| Spring Boot | 3.2.0 | 4.1.0 | ✅ 升级 |
| MySQL | 8.0 | 8.0（保持） | ➖ 不变 |
| ORM | MyBatis-Plus | MyBatis-Plus（保持） | ➖ 不变 |
| Redis | 无 | 8.x | ✅ 引入 |
| 认证 | 单 Token + Cookie | 双 Token | ✅ 改造 |
| AI | 无 | Spring AI + DeepSeek | ✅ 新增 |
| 短信 | 无 | 无（不做） | ➖ 不做 |
| 业主端 UI | Element Plus（PC） | Vant（移动端） | ✅ 改造 |
| 管理端 UI | Element Plus（PC） | Element Plus（保持） | ➖ 不变 |
| 支付 | 支付宝 | 支付宝（保持） | ➖ 不变 |
| 定时任务 | XXL-Job | XXL-Job（保持） | ➖ 不变 |
| 验证码 | 图形验证码 | 图形验证码（保持） | ➖ 不变 |

---

## 3. 各模块详细改造方案

### 3.1 Spring 版本升级（JDK 21→25、Spring Boot 3.2→4.1.0）

#### 改造内容

| 改动项 | 说明 |
|--------|------|
| 根 pom.xml | `maven.compiler.source/target` 21 → 25，`spring-boot.version` 3.2.0 → 4.1.0 |
| 所有模块 pom.xml | 继承根 pom，无需逐个改（依赖版本由 `spring-boot-dependencies` 统一管理） |
| Dockerfile | 基础镜像 `maven:3.9-eclipse-temurin-21` → 25、`eclipse-temurin:21-jre` → 25 |

#### 兼容性风险与处理

| 风险 | 说明 | 处理 |
|------|------|------|
| MyBatis-Plus 兼容性 | 需确认 `mybatis-plus-spring-boot3-starter` 对 Spring Boot 4.x 的兼容版本 | 升级到对应版本，或回退 Spring Boot 到 3.5.x（最新稳定 3.x） |
| knife4j / springdoc | 文档组件需适配 Spring Boot 4.x | 升级到对应版本 |
| API 变化 | Spring Boot 4.x 可能有 API 破坏性变更 | 编译后修复报错点 |

> **务实建议**：Spring Boot 4.1.0 若生态兼容性不理想，可退而求其次升级到 **3.5.x**（3.x 最新稳定版），技术含金量差距不大，但稳定性更高。最终以实际编译结果为准。

---

### 3.2 引入 Redis

#### 用途

| 场景 | 说明 |
|------|------|
| 系统配置缓存 | 现有 `sys_config` 查询改为 Redis 缓存，减少 DB 压力 |
| Token 黑名单 | 配合双 Token，登出/刷新时加入黑名单 |
| 分布式锁 | 替换现有 MySQL `GET_LOCK`，用于缴费/车位并发控制 |

#### 改动点

| 文件/模块 | 改动 |
|-----------|------|
| 根 pom.xml | 引入 `spring-boot-starter-data-redis` |
| 新增 `RedisConfig` | RedisTemplate 序列化配置（Jackson 序列化，Key 用 String） |
| 新增 `RedisUtil` | 封装常用操作（set/get/delete/expire/加锁） |
| 替换 MySQL GET_LOCK | `BillServiceImpl` 等用到 `acquireLock/releaseLock` 的地方改用 Redis 分布式锁 |
| 系统配置缓存 | `sys_config` 查询加缓存 + 失效策略 |
| docker-compose | 增加 Redis 服务（端口 6379） |

#### Redis 数据结构设计

| Key | 类型 | 用途 |
|-----|------|------|
| `token:refresh:{userId}` | String | Refresh Token 存储 |
| `token:blacklist:{token}` | String | Access Token 黑名单 |
| `config:{configKey}` | String | 系统配置缓存 |
| `lock:{业务Key}` | String | 分布式锁 |

---

### 3.3 双 Token 认证

#### Token 设计

| Token | 有效期 | 存储 | 用途 |
|-------|--------|------|------|
| Access Token | 2 小时 | 前端（httpOnly Cookie） | 每次请求鉴权 |
| Refresh Token | 7 天 | 服务端 Redis | 刷新 Access Token |

#### 核心流程

```
登录 → 生成 Access Token + Refresh Token，Refresh Token 存 Redis，Access Token 写 Cookie
↓
请求接口 → 带 Access Token → AuthInterceptor 校验
↓ Access Token 过期（401）
→ 前端调刷新接口 → 后端校验 Refresh Token → 生成新 Access Token（无感刷新）
↓ 登出
→ Refresh Token 从 Redis 删除，Access Token 加入黑名单
```

#### 改动点

| 文件/模块 | 改动 |
|-----------|------|
| `JwtUtil` | 扩展：生成 Access Token（2h）+ Refresh Token（7d），区分类型 |
| 登录接口 | 返回双 Token；Access Token 写 Cookie，Refresh Token 存 Redis |
| 新增 `POST /refresh` 接口 | 校验 Refresh Token，返回新 Access Token |
| `AuthInterceptor` | 校验 Access Token 时增加黑名单检查 |
| 登出接口 | 删除 Refresh Token，Access Token 加入黑名单 |

#### 前端配合

| 端 | 改动 |
|----|------|
| admin-web / owner-web | 401 时自动调刷新接口重试一次，失败再跳登录页 |

---

### 3.4 Spring AI + DeepSeek（AI 物业客服）

#### 场景设计

| 场景 | 示例问题 |
|------|---------|
| 缴费咨询 | "我这个月物业费多少？""怎么缴费？" |
| 业务咨询 | "怎么报修？""车位怎么租？" |
| 通用问答 | 物业相关常见问题 |

#### 技术选型

| 组件 | 选型 | 说明 |
|------|------|------|
| AI 框架 | Spring AI | 官方框架，统一大模型调用 |
| 大模型 | DeepSeek | 兼容 OpenAI 协议，个人可申请 API Key（有免费额度） |
| Starter | `spring-ai-openai-starter` | DeepSeek 走 OpenAI 协议 |
| 流式响应 | SSE | 打字机效果 |

#### 改动点

| 文件/模块 | 改动 |
|-----------|------|
| 新增 `property-module-ai` 模块 | 封装 AI 对话能力 |
| `AiConfig` | 配置 DeepSeek API Key、base-url、模型名 |
| `AiChatService` | 封装对话 + 流式对话方法 |
| 新增接口 | `POST /api/owner/chat`（流式 SSE）+ `GET /api/owner/chat/history` |
| 系统提示词 | 从 `sys_config` 读取，可后台配置 |
| 前端业主端 | AI 客服聊天界面（气泡 + 打字机效果） |

#### 进阶（可选，第二阶段）

- **RAG**：接入向量库，让 AI 基于真实业务数据回答（"我上月物业费 500 元"），而非只会泛泛而谈

---

### 3.5 多端实现（业主端移动化）

#### 目标

- **业主端**：移动端适配，使用 Vant 4 组件库，支持手机登录和手机端操作
- **管理端**：保持 PC 端 Element Plus，不变

#### 改动点

| 端 | 改动 |
|----|------|
| 业主端 owner-web | 引入 `vant`；登录/账单/缴费/支付记录等页面改用 Vant 组件；适配移动端布局 |
| 手机登录 | 已有手机号+密码+图形验证码登录，移动端沿用 |
| 管理端 admin-web | 不变 |

#### 移动端适配要点

- 视图（View）移动端适配（`postcss-px-to-viewport` 或 rem）
- 按钮点击区域 ≥ 48px × 48px
- 底部 TabBar 导航（首页/账单/缴费/我的）

---

## 4. 分阶段实施计划

| 阶段 | 内容 | 依赖 | 验收标准 |
|------|------|------|---------|
| 阶段 1 | Spring 版本升级 + Redis 引入 | 无 | 项目在 JDK 25 + Spring Boot 4.1 下编译运行；Redis 缓存/锁生效 |
| 阶段 2 | 双 Token 认证 | 阶段 1（Redis） | 登录返回双 Token；Access 过期可无感刷新；登出后 Token 失效 |
| 阶段 3 | Spring AI + DeepSeek 流式对话 | 无 | 业主端可流式对话；系统提示词可配置 |
| 阶段 4 | 业主端移动化（Vant） | 无 | 业主端在手机上正常使用 |
| 阶段 5（可选） | RAG 接入 | 阶段 3 | AI 能基于真实业务数据回答 |

---

## 5. 风险与注意事项

1. **Spring Boot 4.1.0 兼容性**：4.x 较新，MyBatis-Plus、knife4j 等依赖需确认兼容版本；如遇不可解决的冲突，回退到 3.5.x。
2. **双 Token 与现有 Cookie 方案的迁移**：现有单 Token + httpOnly Cookie 需平滑迁移，过渡期兼容旧 Token。
3. **DeepSeek API Key**：需申请（个人免费额度），配置通过环境变量注入，不落库。
4. **移动端改造工作量**：业主端页面较多，建议优先核心页面（登录/账单/缴费/记录），其余逐步迁移。
5. **Redis 引入后**：docker-compose 需增加 Redis 服务；本地开发需启动 Redis。

---

## 6. 验收标准

- [ ] 项目在 JDK 25 + Spring Boot 4.1.0 下编译、打包、运行正常
- [ ] Redis 缓存系统配置、Token 黑名单、分布式锁均生效
- [ ] 登录返回双 Token，Access Token 过期后无感刷新，登出后 Refresh Token 失效
- [ ] 业主端 AI 客服支持 SSE 流式对话，打字机效果正常
- [ ] 业主端在手机上正常登录、查账单、缴费、查记录
- [ ] 管理端功能不受影响
