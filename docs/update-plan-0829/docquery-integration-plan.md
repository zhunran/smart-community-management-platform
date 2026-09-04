# DocQuery 集成方案（property-management ↔ DocQuery）

> 日期：2026-08-29
> 状态：计划草案（待评审，暂不执行）
> 目标：将独立 RAG 服务 `DocQuery`（社区规章与信息检索）接入物业管理系统，业主端智能客服与管理端规章检索复用同一套知识库检索能力。

---

## 一、背景与目标

`DocQuery`（`D:\Projects\DocQuery`）是一个**独立部署的 Python FastAPI + Vue 前端**项目，已完成「上宫苑社区规章」知识库的向量化检索与 RAG 问答（17 个 PDF，ChromaDB + bge 中文向量 + Reranker + DeepSeek）。

物业管理系统（Java 单体，13 模块）已有一套 Spring AI 智能客服（`property-module-ai`），但其能力是「查业主实时业务数据」（账单/房屋/公告，基于 `@Tool`），**不具备社区规章知识库检索能力**。

本方案目标：让两个项目联动，实现——

1. **业主端**：智能客服遇到「社区规章类」问题（装修规定、费用标准、宠物管理等）时，调用 DocQuery 的 RAG 检索/问答。
2. **管理端**：在规章制度页面提供「语义检索框」，只检索规章知识库，返回相关条款 + 原文出处。

---

## 二、现状分析

### 2.1 DocQuery（独立服务）

| 维度 | 现状 |
|---|---|
| 技术栈 | Python 3.13 + FastAPI + ChromaDB + SQLite + bge-large-zh-v1.5 + bge-reranker-v2-m3 + DeepSeek |
| 前端 | Vue 3 + Vite + TS（自带 Chat / AdminPanel / CitationPanel） |
| 部署 | Dockerfile + docker-compose（含 Prometheus/Grafana 监控） |
| 知识库 | `shanggongyuan`（上宫苑社区规章，已索引） |
| 已有鉴权 | [auth.py](file:///D:/Projects/DocQuery/backend/app/core/auth.py) `verify_admin_api_key`（`X-API-Key` + `admin_api_key`，`auth_mode=api_key`） |
| 鉴权覆盖 | 仅 `/api/v1/admin/*`（[admin.py](file:///D:/Projects/DocQuery/backend/app/api/admin.py#L15-L18)）；`/search`、`/chat`、`/chat/stream`、`/conversations` **公开无鉴权** |

### 2.2 关键接口（[routes.py](file:///D:/Projects/DocQuery/backend/app/api/routes.py)）

| 端点 | 用途 | 对应场景 |
|---|---|---|
| `POST /api/v1/search` | 纯检索（不生成回答，返回 content/score/source/metadata） | 管理端规章语义检索框 |
| `POST /api/v1/chat` | RAG 问答（一次性） | 业主端非流式 |
| `GET /api/v1/chat/stream` | RAG 流式问答（SSE） | 业主端流式 |
| `POST /api/v1/conversations` 等 | 对话管理 | DocQuery 自带前端使用 |

### 2.3 property-management（Java 单体）

| 维度 | 现状 |
|---|---|
| 鉴权 | JWT 双 token（[JwtUtil](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/JwtUtil.java) / [TokenService](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/TokenService.java) / [AuthInterceptor](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/interceptor/AuthInterceptor.java)），admin/owner 双 secret |
| AI 客服 | [AiChatService](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-ai/src/main/java/com/property/module/ai/service/AiChatService.java)（Spring AI ChatClient + Flux<String> SSE + [CommunityInfoTool](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-ai/src/main/java/com/property/module/ai/tool/CommunityInfoTool.java) 的 @Tool） |
| 入口 | admin-api（管理端）、owner-api（业主端） |

---

## 三、核心决策

### 3.1 鉴权方案：内部服务鉴权，复用 X-API-Key，不加 JWT

- DocQuery 已有 `X-API-Key` 机制，直接复用；给 `/search`、`/chat`、`/chat/stream` 补上鉴权即可。
- **不加 JWT 的理由**：DocQuery 是 Python，验 Java 端 jjwt（双 secret + type claim + Redis 黑名单）需在 Python 重写整套 JWT 体系，成本高且易漂移；S2S 调用只需「证明调用方是自家人」，API Key 足够。
- **网络层**：两服务同 docker-compose 内网，DocQuery 不对外暴露端口，Java 内网访问；密钥走环境变量 `ADMIN_API_KEY`。

### 3.2 两个 AI 的分工

| | property-module-ai | DocQuery |
|---|---|---|
| 职责 | 查业主实时业务数据（@Tool） | 查社区规章知识库（RAG） |
| 载体 | Spring AI + DeepSeek | FastAPI + Chroma + Reranker |
| 联动 | 业主问规章时转调 DocQuery | 独立提供检索/问答 |

---

## 四、实施步骤（分阶段，含验收标准）

### 阶段 1：DocQuery 补鉴权（改动最小，先做）

**改动**：在 [routes.py](file:///D:/Projects/DocQuery/backend/app/api/routes.py) 的 `/search`、`/chat`、`/chat/stream` 端点加 `dependencies=[Depends(verify_admin_api_key)]`（或抽一个受保护子 router）。

**验收标准**：
- 不带 `X-API-Key` 请求 `/search`/`/chat` 返回 401/403。
- 带正确 `X-API-Key` 请求正常返回。
- DocQuery 自带前端（Vue）通过 nginx 反向代理时不受影响（或同步加 header）。

### 阶段 2：Java 侧新增 DocQuery 客户端

**改动**：在 `property-module-ai`（或 framework）新增：
- `DocQueryProperties`（`@ConfigurationProperties`）：`docquery.base-url`、`docquery.api-key`、超时等。
- `DocQueryClient`：封装 HTTP 调用，统一加 `X-API-Key` 头，提供 `search(query, topK)`、`chatStream(query)`。
- 使用 Spring 的 `RestClient`/`WebClient`（流式用 WebClient，与 SSE 契合）。

**验收标准**：
- 配置文件新增 `docquery.*` 配置项，密钥走环境变量，不硬编码。
- 单元测试：`search` 能拿到 DocQuery 返回的结构化结果。

### 阶段 3：管理端「规章语义检索框」

**改动**：
- 后端：admin-api 新增只读接口 `POST /api/admin/regulation/search`（走现有 JWT 鉴权），内部调 `DocQueryClient.search`，返回 `[{条款/内容、相似度、出处}]`。
- 前端：admin-web 规章制度页加搜索框 + 结果列表（命中关键词高亮 + 原文出处）。

**验收标准**：
- 管理端登录后可检索社区规章，结果带原文出处、可高亮。
- 该接口不涉及任何业主数据，无越权风险。

### 阶段 4：业主端 AI 客服接入规章检索

**改动**：
- 在 `AiChatService` 的流程中，当问题命中「社区规章」时，转调 `DocQueryClient.chatStream`（`knowledge_base=shanggongyuan`）流式返回；或把 DocQuery 的检索结果作为上下文拼进现有 prompt。
- 保持现有 `@Tool`（查账单/房屋/公告）能力不变。

**验收标准**：
- 业主问「装修押金多少」能返回基于规章的回答（带出处）。
- 问「我的账单」仍走 @Tool，两条链路互不干扰。

---

## 五、技术要点

1. **流式透传**：Java 用 WebClient 订阅 DocQuery 的 `text/event-stream`，再转成 `Flux<String>` 透传给前端，避免全量缓冲。
2. **鉴权头注入**：`DocQueryClient` 统一在请求里带 `X-API-Key`，密钥从环境变量读取（对齐 property 项目「敏感信息走环境变量」约定）。
3. **超时与降级**：DocQuery 不可用时，AI 客服应降级为「引导联系物业服务中心」，不阻塞主流程。
4. **限流**：DocQuery 已有 chat 20/min、admin 5/min 限流，Java 侧无需重复，但要关注管理端检索频率。
5. **CORS 不变**：服务端到服务端调用，不经过浏览器，无需改 DocQuery 的 CORS。

---

## 六、风险与注意事项

| 风险 | 应对 |
|---|---|
| 两项目当初未设计联动，接口契约需对齐 | 阶段 2 先定 `DocQueryClient` 的请求/响应 DTO，以 DocQuery 现有 Schema 为准 |
| DocQuery `/search` 目前无鉴权 | 阶段 1 优先补上，避免公开裸奔 |
| DeepSeek/Embedding 依赖外网 | DocQuery 已内置模型本地加载 + hf-mirror，沿用即可 |
| 业主端两套 AI（@Tool vs RAG）切换边界不清 | 阶段 4 明确「规章类走 RAG、数据类走 @Tool」的路由规则 |

---

## 七、暂缓项（本计划不覆盖）

- 不做 DocQuery 前端嵌入 property 项目（保持独立）。
- 不做网关（现无网关，为单点集成不值得引入）。
- 不做 JWT 打通（维持 X-API-Key 内部鉴权）。
