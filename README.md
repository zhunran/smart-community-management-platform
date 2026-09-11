# smart-community-management-platform（临宫台智慧社区管理平台）

临宫台智慧社区管理平台 —— 面向中小型社区的智慧物业管理平台，覆盖业主管理、账单催缴、在线支付、停车管理、社区公告、AI 智能客服等核心场景。

## 技术栈

| 层级       | 技术                              | 版本         |
| ---------- | --------------------------------- | ------------ |
| 语言       | JDK                               | **25**       |
| 框架       | Spring Boot                       | 4.1.0        |
| 数据库     | MySQL                             | 8.0+         |
| ORM        | MyBatis-Plus                      | 3.5.16       |
| 缓存       | Redis                             | 7.x          |
| 认证       | JWT 双 Token（管理端 + 业主端）   | jjwt 0.12.5  |
| AI         | Spring AI（DeepSeek，SSE 流式）   | 2.0.0        |
| 任务调度   | XXL-Job                           | 2.4.1        |
| 支付       | 支付宝 SDK                        | 4.39.246.ALL |
| 管理端前端 | Vue 3 + Element Plus + TypeScript | —            |
| 业主端前端 | Vue 3 + Vant 4 + TypeScript       | —            |
| API 文档   | Knife4j (OpenAPI 3)               | 5.0.18       |

## 模块架构

```
property-management（父 POM，JDK 25 / Spring Boot 4.1.0）
├── property-common              公共层：枚举、工具类、异常码、DTO
├── property-framework           框架层：JWT 鉴权、AOP 切面、全局异常、统一响应、配置
├── property-module-housing      业务层：楼栋 / 单元 / 房屋数据访问
├── property-module-owner        业务层：业主信息管理、房屋绑定
├── property-module-bill         业务层：账单生成、逾期催缴、费用标准
├── property-module-payment      业务层：支付中心（支付宝 / 线下）
├── property-module-parking      业务层：停车位管理、停车卡、租赁合同
├── property-module-notification 业务层：邮件通知、社区公告
├── property-module-statistic    业务层：财务报表统计、操作审计
├── property-module-ai           业务层：AI 客服（DeepSeek + Function Calling）
├── property-module-community    业务层：社区活动、论坛、投票
├── property-module-lifeservice  业务层：报修工单、访客通行、场地预约
├── property-admin-api           管理端 API（端口 8081）
├── property-owner-api           业主端 API（端口 8084）
├── property-task                定时任务执行器（XXL-Job，端口 8083）
├── property-admin-web           管理端前端（Vue3 + Element Plus）
└── property-owner-web           业主端前端（Vue3 + Vant 移动端）
```

共 15 个 Maven 子模块，按职责分为四层：

| 分层   | 模块                                                                                        | 职责                                    |
| ------ | ------------------------------------------------------------------------------------------- | --------------------------------------- |
| 公共层 | common, framework                                                                           | 数据定义、基础设施（JWT/AOP/异常/配置） |
| 业务层 | housing, owner, bill, payment, parking, notification, statistic, ai, community, lifeservice | 各业务域的 Entity/Mapper/Service        |
| 入口层 | admin-api, owner-api, task                                                                  | REST API 聚合 + 定时任务执行器          |
| 前端层 | admin-web, owner-web                                                                        | PC 管理后台 + 移动端业主端              |

## 系统架构图

```
┌────────────────────────────────────────────────────────────────┐
│                       前端层（Frontend）                        │
│  ┌────────────────────────┐    ┌────────────────────────┐      │
│  │  property-admin-web    │    │  property-owner-web    │      │
│  │  PC 管理后台            │    │  移动端 H5 业主端        │      │
│  │  Vue3 + Element Plus   │    │  Vue3 + Vant 4         │      │
│  └───────────┬────────────┘    └───────────┬────────────┘      │
└──────────────┼─────────────────────────────┼───────────────────┘
               │   HTTP + httpOnly Cookie 鉴权
┌──────────────▼─────────────────────────────▼───────────────────┐
│                      入口层（API Entry）                        │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐       │
│  │  admin-api    │  │  owner-api    │  │  task         │       │
│  │  管理端 :8081  │  │  业主端 :8084  │  │  XXL-Job :8083│       │
│  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘       │
└──────────┼──────────────────┼──────────────────┼───────────────┘
           ▼                  ▼                  ▼
┌────────────────────────────────────────────────────────────────┐
│              框架层 property-framework（横切能力）               │
│  JWT 双 Token 鉴权 │ @SkipAuth 放行 │ 全局异常 │ 统一响应        │
│  @OperationLog 异步审计 │ TraceFilter 链路追踪 │ Redis 工具     │
├────────────────────────────────────────────────────────────────┤
│              公共层 property-common（纯数据定义）                │
│  ApiResult │ 异常体系 │ 枚举 │ LoginUser │ PageQuery            │
├────────────────────────────────────────────────────────────────┤
│                    业务模块层（10 个业务域）                      │
│  ┌────────┐ ┌──────┐ ┌───────┐ ┌─────────┐ ┌────────┐          │
│  │housing │ │ bill │ │ owner │ │ payment │ │parking │          │
│  │房产     │ │账单  │ │业主    │ │支付      │ │车位    │          │
│  └────────┘ └──────┘ └───────┘ └─────────┘ └────────┘          │
│  ┌──────────┐ ┌───────────┐ ┌──────────┐ ┌────────────┐        │
│  │notification│ │ statistic │ │   ai     │ │ community  │        │
│  │通知        │ │统计审计    │ │AI 客服    │ │社区互动     │        │
│  └──────────┘ └───────────┘ └──────────┘ └────────────┘        │
│                        ┌──────────────┐                        │
│                        │ lifeservice  │                        │
│                        │ 便民服务      │                        │
│                        └──────────────┘                        │
├────────────────────────────────────────────────────────────────┤
│                        基础设施层                                │
│  MySQL 8 │ Redis 7 │ XXL-Job Admin │ 支付宝 │ DocQuery RAG(可选)│
└────────────────────────────────────────────────────────────────┘
```

> 依赖方向单向化：业务模块 → framework → common；管理端 / 业主端独立部署、独立 JWT 密钥，工程级权限隔离。

## 核心功能

### 管理端

- 楼栋 / 单元 / 房屋 / 业主信息 CRUD，Excel 批量导入导出
- 费用标准配置、账单批量生成、缴费记录查询
- 停车位分配管理、停车卡管理
- 公告发布 / 下线 / 删除
- 财务报表（收入总览 / 收费率 / 欠费排行）
- 操作数据看板

### 业主端

- 查看名下房屋、账单、缴费记录
- 支付宝在线缴费
- 社区公告查看
- AI 智能客服（流式 SSE 对话，支持查询账单、房屋、公告等）

### 社区互动

- 社区活动：活动发布 / 取消、报名 / 取消报名、我的报名
- 论坛：帖子发布与审核、两级评论树（父评论 + 子回复）、点赞去重、置顶 / 精华 / 软删除
- 社区投票：投票发起 / 开始 / 结束、业主投票、结果统计

### 便民服务

- 报修工单：提交 → 审核 → 派单 → 接单 → 完成 → 评价 全流程，支持工单统计
- 访客邀请：业主生成访客通行证，门岗核验
- 场地预约：场地管理、可预约时段查询、在线预约 / 取消

### 自动化

- 每月定时生成账单（XXL-Job）
- 逾期账单邮件催缴
- 支付宝对账定时任务

## 业务流程图

### 1. 核心运营闭环（计费 → 收缴 → 催缴 → 对账 → 审计）

```
资源管理（楼栋 / 单元 / 房屋 / 业主绑定）
        │
        ▼
计费 ── 4 种计费模型（按面积/按户/按用量/固定）
   │     账单幂等生成（同房同期非终态跳过，任务可安全重跑）
   │     XXL-Job 每月 1 日自动执行
        ▼
收缴 ── 支付宝在线缴费 / 管理端线下缴费（现金、转账）
   │     回调四层防重 + 主动对账兜底（见流程 2）
        ▼
催缴 ── XXL-Job 每日扫描逾期账单
   │     自动标记 OVERDUE + 计算罚息（上限 100%，费率热配置）
   │     按业主分组发送催缴邮件（Thymeleaf 模板）
        ▼
治理 ── 主动对账（支付单 + 车位双轨比对预警）
        操作审计留痕 + 经营报表（收缴率 / 欠费 / 缴费趋势）
```

### 2. 支付链路（四层防重 + 主动对账）

```
业主多选账单 ──► 下单（待支付订单复用，防重复建单）
                     │
                     ▼
              跳转支付宝收银台 ──► 支付成功
                     │
                     ▼ 异步回调（至少一次投递）
  ┌──────────────────────────────────────────────────┐
  │ ① RSA2 验签（仅处理 TRADE_SUCCESS/FINISHED）       │
  │ ② Redis 分布式锁（防多实例并发）                    │
  │ ③ 行级锁 FOR UPDATE（事务内串行化）                │
  │ ④ 幂等检查（终态直接返回成功）                      │
  │ ⑤ 乐观锁更新（WHERE status=?，恰好一次生效）        │
  └──────────────────────────────────────────────────┘
                     │
                     ▼
        账单已缴金额更新 → 状态重算（UNPAID/PARTIAL/PAID）→ 明细分摊

  【兜底】回调丢失时：定时对账扫描 30 分钟前待确认订单
         → 主动查询支付宝 → 按结果补偿纠正，保证资金最终一致
```

### 3. 报修工单流转（8 态状态机 + 行级锁串行）

```
                    ┌──审核驳回──► 已驳回
                    │
业主提交 ──► 待审核 ─┤
                    └──审核通过──► 待派单 ──管理端派单──► 已派单
                                                    │ 维修员接单
                                                    ▼
  业主评价 ◄──业主确认── 已完成 ◄──维修员完工── 维修中

  （任意待处理状态可业主取消 ──► 已取消）
   关键流转（审核/派单/接单/完工）用 FOR UPDATE 行级锁防重复派单/丢单
```

### 4. AI 客服意图分流（Function Calling + 可选 RAG）

```
业主提问 ──► 意图路由（关键词，零 LLM 开销）
              │
              ├─ 业务数据类（物业费/账单/车位/公告…）
              │        └──► Function Calling 查库 ──► 真实数据回答
              │
              ├─ 规章流程类（装修/报修流程/收费标准…）
              │        └──► DocQuery RAG 检索社区规章知识库（可选）
              │                  └─ 异常/宕机 ──► onErrorResume 回退主链路
              │
              └─ 其他（闲聊）──► 主链路（ChatClient + 会话记忆）

              ──► 统一 SSE 流式输出（打字机效果）
```

## 快速开始

### 环境要求

- JDK 25
- Maven 3.8+
- MySQL 8.0+
- Redis 7.x
- Node.js 18+（前端，可选）

### 1. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS property_management
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
```

导入 `property_management.sql` 初始化表结构。

### 2. 配置环境变量

```bash
# 必须
MYSQL_USER=root
MYSQL_PASSWORD=your_password
JWT_ADMIN_SECRET=<64字符随机Base64>
JWT_OWNER_SECRET=<不同的64字符随机Base64>
JWT_EXPIRATION=86400000

# 可选：支付宝
ALIPAY_APP_ID=xxx
ALIPAY_GATEWAY=https://openapi-sandbox.dl.alipaydev.com/gateway.do
ALIPAY_APP_PRIVATE_KEY=...
ALIPAY_ALIPAY_PUBLIC_KEY=...

# 可选：AI 客服
DEEPSEEK_API_KEY=sk-xxx

# 可选：DocQuery RAG（社区规章知识库问答，默认关闭）
DOCQUERY_API_KEY=your_docquery_api_key   # 需与 DocQuery 服务端 ADMIN_API_KEY 一致
```

### 3. 编译启动

```bash
# 编译
mvn clean compile

# 启动管理端（端口 8081）
mvn spring-boot:run -pl property-admin-api

# 启动业主端（端口 8084）
mvn spring-boot:run -pl property-owner-api

# 启动定时任务（端口 8083，需先启动 XXL-Job）
mvn spring-boot:run -pl property-task
```

### 4. 启动前端

```bash
cd property-admin-web && npm install && npm run dev   # 管理端 :5173
cd property-owner-web && npm install && npm run dev   # 业主端 :5273
```

### 5. 访问

| 服务             | 地址                                                  |
| ---------------- | ----------------------------------------------------- |
| 管理端 API 文档  | http://localhost:8081/doc.html                        |
| 业主端 API 文档  | http://localhost:8084/doc.html                        |
| 管理端前端       | http://localhost:5173                                 |
| 业主端前端       | http://localhost:5273                                 |
| XXL-Job 调度中心 | http://localhost:9099/xxl-job-admin（admin / 123456） |

## AI 客服功能

业主端 AI 客服基于 **Spring AI + DeepSeek**，通过函数调用（Function Calling）自动查询真实业务数据：

| 工具     | 能力                         |
| -------- | ---------------------------- |
| 账单查询 | 查询名下未缴 / 已缴账单      |
| 房屋查询 | 查看名下房屋信息             |
| 公告查询 | 获取最新社区公告             |
| 简报生成 | 社区简报（缴费率、通知数等） |

对话记忆由 Redis 持久化，支持 SSE 流式输出。

### DocQuery RAG 增强（可选）

AI 客服集成了自研 RAG 服务 [DocQuery](https://github.com/zhunran/DocQuery)，用于社区规章/流程类问题的知识库问答：

- **意图路由**：关键词粗分流——业务查询（账单/房屋/公告）走主链路查库，规章流程咨询（装修/报修流程/收费标准等）走 DocQuery RAG
- **硬约束**：DocQuery 宕机/异常时通过 `onErrorResume` 自动回退主链路，前端始终收到完整回答，绝无空白
- **配置**：`property-owner-api/src/main/resources/application.yml` 中 `docquery.enabled=true` 开启，`docquery.base-url` 指向 DocQuery 服务地址

| 配置项                    | 默认值                  | 说明                                          |
| ------------------------- | ----------------------- | --------------------------------------------- |
| `docquery.enabled`        | `false`                 | 是否启用 RAG 分支                             |
| `docquery.base-url`       | `http://localhost:8000` | DocQuery 服务地址                             |
| `docquery.api-key`        | `${DOCQUERY_API_KEY:}`  | API Key（需与 DocQuery `ADMIN_API_KEY` 一致） |
| `docquery.knowledge-base` | `shanggongyuan`         | 知识库名称                                    |
| `docquery.rag-mode`       | `basic`                 | RAG 模式（basic/hyde/crag/self_rag）          |
| `docquery.read-timeout`   | `30000`                 | 流式读取超时（ms，推理模型建议 ≥60000）       |

## License

MIT
