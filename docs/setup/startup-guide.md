# 物业管理系统 — 启动指南

> 更新日期：2026-08-08
> 适用版本：JDK 21 + Spring Boot 3.2 + MyBatis-Plus 3.5.7

---

## 一、环境需求

### 1.1 基础运行环境

| 依赖 | 版本要求 | 说明 |
|------|---------|------|
| JDK | **21** | OpenJDK / Oracle JDK / Microsoft JDK 均可 |
| Maven | 3.8+ | 构建工具 |
| MySQL | 8.0+ | 需创建 `property_management` 和 `xxl_job` 两个数据库 |
| Node.js | 18+ / 20 | 前端构建（可选，不启动前端则不需要） |
| npm / pnpm | 9+ | 前端包管理器 |

### 1.2 数据库初始化

```sql
-- 1. 创建业务数据库
CREATE DATABASE IF NOT EXISTS property_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 2. 创建 XXL-Job 数据库
CREATE DATABASE IF NOT EXISTS xxl_job
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

**初始化表结构**：
- `property_management` 数据库：导入项目根目录下的 `property_management.sql`
- `xxl_job` 数据库：导入 XXL-Job 官方的 `tables_xxl_job.sql`（XXL-Job 2.4.0 版本）

### 1.3 第三方服务（可选）

| 服务 | 说明 | 默认配置 | 影响模块 |
|------|------|---------|---------|
| **XXL-Job 调度中心** | 分布式任务调度平台 | 独立部署于 `:9099` | property-task |
| **支付宝沙箱** | 在线支付测试环境 | 需注册蚂蚁金服开放平台 | payment 模块 |
| **QQ 邮箱 SMTP** | 逾期催缴通知邮件 | 需开启 SMTP 服务获取授权码 | property-task |

---

## 二、模块架构总览

### 2.1 Maven 模块结构

```
property-management（父 POM）
├── property-common              公共层：枚举、工具类、异常码
├── property-framework           框架层：JWT 鉴权、AOP、全局异常、响应封装
├── property-module-owner        业务层：业主管理
├── property-module-bill         业务层：账单管理
├── property-module-payment      业务层：支付中心（支付宝/线下）
├── property-module-parking      业务层：停车位管理
├── property-module-notification 业务层：邮件通知
├── property-module-statistic    业务层：财务统计
├── property-admin-api           管理端 API（端口 8081）
├── property-owner-api           业主端 API（端口 8082）
├── property-task                定时任务执行器（端口 8083）
└── xxl-job-admin                XXL-Job 调度中心（端口 9099）
```

### 2.2 组件扫描范围

| 启动类 | 端口 | 扫描包 | 依赖模块 |
|--------|------|--------|---------|
| `PropertyAdminApplication` | 8081 | framework, adminapi, module(owner/bill/payment/parking) | 管理端业务全量 |
| `PropertyOwnerApplication` | 8082 | `com.property`（全量扫描） | 所有模块 |
| `PropertyTaskApplication` | 8083 | common, framework, 全部 module, 两个 api | XXL-Job 执行器 |

### 2.3 端口占用一览

| 服务 | 端口 | 是否必须 |
|------|------|---------|
| property-admin-api | 8081 | 是 |
| property-owner-api | 8082 | 否（无业主端功能时可跳过） |
| property-task | 8083 | 否（无定时任务时可跳过） |
| XXL-Job 调度中心 | 9099 | 否（task 模块启动时检查） |
| XXL-Job 执行器通讯端口 | 9999 | `task` 模块自动注册 |

---

## 三、启动步骤

### 3.1 第一步：配置环境变量

复制 `.env.example` 为 `.env`，按需填写。**最少必须配置**：

```
MYSQL_USER=root
MYSQL_PASSWORD=your_password
JWT_ADMIN_SECRET=<64字符随机Base64>
JWT_OWNER_SECRET=<不同的64字符随机Base64>
JWT_EXPIRATION=86400000
```

**JWT 密钥生成**（PowerShell）：
```powershell
$bytes = New-Object byte[] 64; [Security.Cryptography.RNGCryptoServiceProvider]::new().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

### 3.2 第二步：在 IntelliJ IDEA 中配置 Run Configuration

由于项目使用环境变量注入，需在 IDEA 运行配置中添加以下变量：

#### PropertyAdminApplication（管理端 :8081）

```
MYSQL_USER=root;MYSQL_PASSWORD=your_pwd;JWT_ADMIN_SECRET=<admin密钥>;JWT_EXPIRATION=86400000;SWAGGER_ENABLED=true;MYBATIS_LOG_IMPL=org.apache.ibatis.logging.stdout.StdOutImpl
```

#### PropertyOwnerApplication（业主端 :8082）

```
MYSQL_USER=root;MYSQL_PASSWORD=your_pwd;JWT_OWNER_SECRET=<owner密钥>;JWT_EXPIRATION=86400000;SWAGGER_ENABLED=true;MYBATIS_LOG_IMPL=org.apache.ibatis.logging.stdout.StdOutImpl
```

#### PropertyTaskApplication（定时任务 :8083）

```
MYSQL_USER=root;MYSQL_PASSWORD=your_pwd;XXL_JOB_ADMIN_ADDRESSES=http://localhost:9099/xxl-job-admin;XXL_JOB_ACCESS_TOKEN=your_token;MYBATIS_LOG_IMPL=org.apache.ibatis.logging.stdout.StdOutImpl
```

> **注意**：如果支付宝、邮件等功能尚未配置相关环境变量（如 `ALIPAY_APP_ID`、`MAIL_PASSWORD`），对应的 Service Bean 会被条件注解自动跳过，不影响启动。

### 3.3 第三步：Maven 编译

```bash
# 全量编译
mvn clean compile

# 或仅编译指定模块（含依赖）
mvn clean compile -pl property-admin-api -am
```

### 3.4 第四步：启动各模块

**推荐启动顺序**：

1. **XXL-Job 调度中心**（可选，若不需要定时任务可跳过）
   - 方式一：直接运行 `xxl-job-admin` 模块
   - 方式二：下载 XXL-Job 2.4.0 官方 JAR 独立运行
   - 访问：`http://localhost:9099/xxl-job-admin`（默认账号 admin/123456）

2. **PropertyAdminApplication**（管理端后台）
   - API 文档：`http://localhost:8081/doc.html`（需 `SWAGGER_ENABLED=true`）
   - Swagger UI：`http://localhost:8081/swagger-ui/index.html`

3. **PropertyOwnerApplication**（业主端后台，可选）
   - 端口：8082

4. **PropertyTaskApplication**（定时任务执行器，可选）
   - 启动后自动向 XXL-Job 调度中心注册

### 3.5 第五步：启动前端（可选）

```bash
# 管理端前端
cd property-admin-web
npm install
npm run dev          # 默认 :5173

# 业主端前端
cd property-owner-web
npm install
npm run dev          # 默认 :5273
```

---

## 四、开发环境完整配置速查

### 4.1 环境变量总览矩阵

```
                     AdminAPI  OwnerAPI   Task     说明
───────────────────────────────────────────────────────────
MYSQL_USER            ✅        ✅        ✅      数据库用户名
MYSQL_PASSWORD        ✅        ✅        ✅      数据库密码
JWT_ADMIN_SECRET      ✅        —         —       管理端JWT密钥
JWT_OWNER_SECRET      —         ✅        —       业主端JWT密钥
JWT_EXPIRATION        ✅        ✅        —       Token有效期(ms)
ALIPAY_APP_ID         ○         ○         —       支付宝AppId
ALIPAY_GATEWAY        ○         ○         —       支付宝网关
ALIPAY_*_KEY          ○         ○         —       支付宝公私钥
ALIPAY_NOTIFY_URL     ○         ○         —       支付回调地址
ALIPAY_RETURN_URL     ○         ○         —       支付完成跳转
MAIL_USERNAME         —         —         ○       发件邮箱
MAIL_PASSWORD         —         —         ○       SMTP授权码
XXL_JOB_ADMIN_ADDRS   —         —         ✅      调度中心地址
XXL_JOB_ACCESS_TOKEN  —         —         ✅      调度通信令牌
XXL_JOB_EXECUTOR_IP   —         —         ○       执行器IP
SWAGGER_ENABLED       ○         ○         —       API文档开关
MYBATIS_LOG_IMPL      ○         ○         ○       SQL日志开关
LOG_PATH              —         —         ○       日志路径

✅ 必填    ○ 可选    — 不需要
```

### 4.2 功能开关说明

| 开关 | 开发环境 | 生产环境 |
|------|---------|---------|
| `SWAGGER_ENABLED` | `true` | `false` |
| `MYBATIS_LOG_IMPL` | `StdOutImpl` | 不设置（默认 NoLoggingImpl） |

---

## 五、已知问题与解决方案

### 5.1 MapStruct 编译 stub 问题（IDE 编译冲突）

**现象**：启动报 `No qualifying bean of type 'OwnerConverter' available`

**根因**：IDE 自动编译（IntelliJ 编译器 / VS Code JDT Language Server）产生的 `OwnerConverterImpl.class` 是带有 "Unresolved compilation problems" 的错误 stub，覆盖了 Maven 的正确输出。

**解决**：
- **方案 A（推荐）**：在使用 IDE 运行前先执行 `mvn clean compile`，确保 class 文件正确
- **方案 B**：在 IntelliJ 中设置 `Build → Rebuild Project` 代替 `Build Project`
- **方案 C**：在 IntelliJ 设置中勾选 `Settings → Build Tools → Maven → Runner → Delegate IDE build actions to Maven`
- **方案 D**：在 VS Code 中禁用 `java.autobuild.enabled`
- **方案 E**：删除错误的 class 文件后重新编译

### 5.2 PaymentCallbackService / PaymentOrderService 注入 AlipayService 失败

**现象**（仅 PropertyTaskApplication）：`required a bean of type 'AlipayService' that could not be found`

**根因**：`AlipayService` 有 `@ConditionalOnProperty(name = "alipay.app-id")`，task 模块无需配置支付宝，但 `PaymentCallbackService` 和 `PaymentOrderService` 原来无条件注入 `AlipayService`。

**已修复**（2026-08-08）：为这两个 Service 添加了 `@ConditionalOnBean(AlipayService.class)`，仅在 `AlipayService` 存在时才创建。

### 5.3 数据库连接失败

**现象**：`CommunicationsException: Communications link failure`

**检查清单**：
- [ ] MySQL 服务是否启动
- [ ] `MYSQL_USER` / `MYSQL_PASSWORD` 环境变量是否正确设置
- [ ] `property_management` 数据库是否已创建并导入 SQL
- [ ] 数据库连接 URL 中的 `serverTimezone=Asia/Shanghai` 是否正确

### 5.4 JWT 密钥未配置

**现象**：启动时 `Could not resolve placeholder 'JWT_ADMIN_SECRET'`

**解决**：在 IDEA Run Configuration 的 Environment variables 中添加对应的 JWT 环境变量。

### 5.5 XXL-Job 连接失败

**现象**：task 模块启动后日志显示 `xxl-job registry fail`

**影响**：不影响 task 模块启动，仅定时任务无法被执行。

**解决**：确保 XXL-Job 调度中心已启动，且 `XXL_JOB_ACCESS_TOKEN` 与调度中心配置一致。

### 5.6 支付宝沙箱未配置

**现象**：不影响启动，仅支付功能无法使用

**解决**：参考 `.env.example` 配置支付宝相关环境变量，或跳过支付功能。

---

## 六、相关文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 启动指南（本文） | `docs/startup-guide.md` | 环境搭建、启动步骤、常见问题 |
| 环境变量配置 | `docs/env-config.md` | 各模块环境变量详细对照表 |
| 执行计划 | `docs/execution-plan.md` | 42+ 项优化任务的时间线和执行顺序 |
| 实施记录 | `docs/summary.md` | 已完成的任务和修改清单 |
| 审查报告 | `docs/code-review-summary.md` | 42+ 项问题的分级审查 |
| 简历项目经历 | `docs/resume-project.md` | 用于面试展示的项目总结 |
| 环境变量模板 | `.env.example` | 环境变量配置模板（不纳入版本控制） |

---

## 七、快速启动脚本

### PowerShell 一键编译启动（管理端）

```powershell
# 配置环境变量
$env:MYSQL_USER = "root"
$env:MYSQL_PASSWORD = "your_pwd"
$env:JWT_ADMIN_SECRET = "your_jwt_secret"
$env:JWT_EXPIRATION = "86400000"
$env:SWAGGER_ENABLED = "true"
$env:MYBATIS_LOG_IMPL = "org.apache.ibatis.logging.stdout.StdOutImpl"

# 编译
mvn clean compile -pl property-admin-api -am -q

# 启动
mvn spring-boot:run -pl property-admin-api
```

### 一键完整启动（所有后端 + 前端）

```powershell
# 1. 启动 MySQL（确保服务运行中）
# 2. 编译
mvn clean compile -q

# 3. 启动 XXL-Job（新终端）
mvn spring-boot:run -pl xxl-job-admin

# 4. 启动管理端（新终端）
mvn spring-boot:run -pl property-admin-api

# 5. 启动业主端（新终端）
mvn spring-boot:run -pl property-owner-api

# 6. 启动定时任务（新终端）
mvn spring-boot:run -pl property-task

# 7. 启动前端（新终端）
cd property-admin-web ; npm run dev
cd property-owner-web ; npm run dev
```
