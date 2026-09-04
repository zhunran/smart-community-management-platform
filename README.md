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

> RAG 检索增强暂搁置，后续可通过取消注释 `AiConfig` 和 `pom.xml` 中标记的代码恢复。

## 项目文档

| 文档         | 路径                                                    |
| ------------ | ------------------------------------------------------- |
| 业务全景     | `docs/summary/业务全景.md`                              |
| 架构设计     | `docs/summary/架构设计.md`                              |
| 技术栈       | `docs/summary/技术栈.md`                                |
| 关键实现逻辑 | `docs/summary/关键实现逻辑.md`                          |
| 启动指南     | `docs/setup/startup-guide.md`                           |
| 升级计划     | `docs/update-plan-0816/upgrade-plan.md`                 |
| AI 增强手册  | `docs/update-plan-0816/phase5-ai-enhancement-manual.md` |
| AI 修复指南  | `docs/update-plan-0816/ai-fix-guide.md`                 |
| 面试问题清单 | `docs/summary/面试问题清单.md`                          |

## License

MIT
