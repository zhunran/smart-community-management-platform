# 物业管理信息系统 — 全面优化执行计划

> 包含：问题修复（42+ 项）+ 能力增强（6 大项）
> 计划编制日期：2026-07-24

---

## 总览

| | 范围 | 任务数 | 预估工期 |
|---|------|--------|---------|
| 第一阶段 | 🔴 安全修复 + 关键 Bug | 12 | 2-3 天 |
| 第二阶段 | 🟠 数据正确性 + 性能优化 | 18 | 3-4 天 |
| 第三阶段 | 🟡 代码质量 + 设计优化 | 10 | 2-3 天 |
| 第四阶段 | 🟢 前端体验 + 数据库规范 | 12 | 2-3 天 |
| 第五阶段 | 🚀 生产就绪 | 10 | 4~7 天 |
| **合计** | | **约 62 项** | **13~21 天** |

---

# 第一阶段：🔴 安全修复 + 关键 Bug

> 目标：消除最严重的安全风险，修复会导致资金/数据错误的 Bug
> 预估：2-3 天

---

### 1.1 安全基础设施

| # | 任务 | 涉及文件 | 操作 | 验证方式 |
|---|------|---------|------|---------|
| 1 | 统一数据库密码为环境变量 | `property-owner-api/application.yml` L9-L10, `xxl-job-admin/application-prod.yml` | 将所有明文密码替换为 `${MYSQL_USER}` / `${MYSQL_PASSWORD}` | 启动时检查连接成功 |
| 2 | JWT Secret 区分管理端/业主端 | `JwtUtil.java` L25, 两个 application.yml | 生成两个独立的 256-bit 密钥，通过 `${JWT_ADMIN_SECRET}` / `${JWT_OWNER_SECRET}` 注入，删除 @Value 默认值 | Token 跨端解析失败 |
| 3 | 支付宝商户私钥移出 classpath | `AlipayConfig.java` L21-L34 | 删除 `alipay-sandbox.properties` 文件，通过 `${ALIPAY_PRIVATE_KEY}` 注入，将该文件加入 `.gitignore` | 沙箱环境支付正常 |
| 4 | 邮件密码环境变量化 | `property-task/application.yml` L11 | 替换为 `${MAIL_PASSWORD}` | 邮件发送正常 |
| 5 | XXL-Job accessToken 环境变量化 | `property-task/application.yml` L55 | 替换为 `${XXL_JOB_ACCESS_TOKEN}` | 任务调度正常 |
| 6 | XXL-Job IP/地址环境变量化 | `property-task/application.yml` L47,L51 | IP 和调度中心地址均替换为环境变量 | 网络连通正常 |
| 7 | XXL-Job 日志路径相对化 | `property-task/application.yml` L53 | 硬编码绝对路径改为 `./logs` | 日志文件正常生成 |
| 8 | Swagger 仅 dev 环境开启 | `application.yml` L26-L39 | `springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}`，部署时 prod 环境自动关闭 | prod 环境 404 |
| 9 | SQL 日志仅在 dev 环境输出 | `application.yml` L23-L24 | 改为 Slf4jImpl + profile 控制日志级别 | prod 环境无 SQL 打印 |

### 1.2 关键数据正确性 Bug

| # | 任务 | 涉及文件 | 操作 | 验证方式 |
|---|------|---------|------|---------|
| 10 | 修复支付方式名称映射不一致 | `property-module-bill/.../PaymentOrderServiceImpl.java` L28 | 统一两个模块的数组定义，或提取为公共枚举/常量 | 不同模块 method=1 显示相同名称 |
| 11 | 修复 manualPayment 返回错误 ID | `BillServiceImpl.java` L338-L363 | 保存生成 ID → 插入时使用 → 返回同一 ID | 返回值查数据库可找到记录 |
| 12 | 修复线下缴费并发超收风险 | `PaymentOrderService.java` L46-L115 | 事务内 `SELECT ... FOR UPDATE` 锁定账单行，或用乐观锁 version 字段 | 并发模拟不超收 |

---

# 第二阶段：🟠 数据正确性 + 性能优化

> 目标：修复业务逻辑缺陷，解决 N+1 查询等性能瓶颈
> 预估：3-4 天

---

### 2.1 业务逻辑修复

| # | 任务 | 涉及文件 | 操作 | 验证方式 |
|---|------|---------|------|---------|
| 13 | 账单幂等检查补上 DISCOUNTED 状态 | `BillServiceImpl.java` L120-L129 | 排除条件扩大到 VOIDED + DISCOUNTED | 已减免账单不重复生成 |
| 14 | 支付时间解析失败不吞数据 | `PaymentCallbackTxService.java` L126-L133, `PaymentReconciliationTxService.java` L130-L137 | 解析失败返回 null 而非 now()，调用方兼容 null | 对账数据时间准确 |
| 15 | updatePaymentSuccess 方法重命名 | `PaymentCallbackTxService.java` L116-L124 | 重命名为 updatePaymentStatus() 或拆分成功/失败两个方法 | 语义清晰 |
| 16 | 业主 update 空指针修复 | `OwnerServiceImpl.java` L124-L126 | `entity.getIdCardNo()` → `Objects.equals()` | null 值不报异常 |
| 17 | OwnerEntity password 防序列化泄露 | `OwnerEntity.java` L22-L23 | 添加 `@JsonIgnore` 注解 | API 响应不含 password |
| 18 | insertPayment 移除硬编码 'admin' | `BillPaymentMapper.java` L29 | 删除 SQL 中的 `'admin'`，依赖 MetaObjectHandler 自动填充 | create_by 为真实登录用户 |
| 19 | 车位操作增加外键校验 | `ParkingSpaceServiceImpl.java` L39-L75 | bind/change 之前查询业主/房屋是否存在 | 不存在时拒绝操作 |
| 20 | 车位对账增加去重机制 | `ParkingReconciliationServiceImpl.java` L48-L74 | 插入前查询同 spaceId+warningType 的未关闭预警 | 不产生重复预警 |
| 21 | 对账事务拆分为小事务 | `ParkingReconciliationServiceImpl.java` L49 | 将批量 INSERT 分批提交，每批独立事务 | 大事务不长时间占连接 |
| 22 | 默认密码生成安全化 | `OwnerServiceImpl.java` L91-L93 | 用 SecureRandom 生成6位密码 + 增加 phone 长度校验 | 密码安全可被接受 |

### 2.2 性能优化

| # | 任务 | 涉及文件 | 操作 | 验证方式 |
|---|------|---------|------|---------|
| 23 | N+1 查询优化（账单分页） | `BillServiceImpl.java` L547-L562 | 用一次 IN 查询批量获取所有房屋楼栋信息 | 分页 SQL 从 40+ 降至 3 次 |
| 24 | N+1 查询优化（支付记录） | `PaymentOrderServiceImpl.java` L84-L105 | 批量预查询房屋、业主、账单信息，Map 缓存填充 | 分页 SQL 从 120+ 降至 4-5 次 |
| 25 | N+1 查询优化（业主房间） | `OwnerRoomServiceImpl.java` L121-L142 | 批量查询业主信息，Map 缓存填充 | SQL 从 N+1 降至 2 次 |
| 26 | N+1 查询优化（单元/房间 API） | UnitServiceImpl, RoomServiceImpl | 批量查询关联数据 | 告别逐条查库 |
| 27 | 账单编号生成改为序列表 | `BillServiceImpl.java` L265-L284 | 用自增序列表或 Redis INCR 替代 LIKE 模糊查询 | 高并发下号码唯一且快 |
| 28 | 支付单号生成并发安全 | `PaymentOrderService.java` L138-L151 | 同 #27 策略 | 并发测试不下重复 |
| 29 | SysConfigService 加缓存 | `SysConfigService.java` L71-L78 | @Cacheable 注解，Redis 或本地缓存，配置变更时失效 | 第二次查询不走库 |
| 30 | 报告导出改为流式写入 | `ReportExportService.java` | 分页查询 + 流式写入 EasyExcel，不当内存中堆积 | 10 万行不 OOM |

---

# 第三阶段：🟡 代码质量 + 设计优化

> 目标：消除代码冗余、统一风格、提升可维护性
> 预估：2-3 天

---

### 3.1 消除代码冗余

| # | 任务 | 涉及文件 | 操作 |
|---|------|---------|------|
| 31 | 合并线下缴费重复实现 | BillServiceImpl + PaymentOrderService | BillServiceImpl 委托 PaymentOrderService，删除 BillServiceImpl 中的 manualPayment/itemizedPayment 核心逻辑 |
| 32 | 消除 TxService 重复代码 | PaymentCallbackTxService + PaymentReconciliationTxService | 抽取公共方法（parsePaymentTime、markAsFailed）到抽象父类或工具类 |
| 33 | 统一日志声明风格 | BillServiceImpl.java L46 | 用 @Slf4j 替代手动 LoggerFactory |

### 3.2 设计缺陷修复

| # | 任务 | 涉及文件 | 操作 |
|---|------|---------|------|
| 34 | MySQL GET_LOCK 改为 Redis 分布式锁 | `PaymentCallbackService.java` L77-L91 | 引入 Redisson，替代连接级命名锁 |
| 35 | AlipayApiException 改为业务异常 | `AlipayService.java` L66-L74, L91-L99 | 抛自定义异常携带 errCode/errMsg，不简单包装 RuntimeException |
| 36 | ResponseAdvice String 返回值安全处理 | `ResponseAdvice.java` L43 | beforeBodyWrite 中增加 String 类型判断 |
| 37 | JacksonConfig Long→String 精确控制 | `JacksonConfig.java` L42-L43 | 不全局转换，只对业务需要的 ID 字段注解 |
| 38 | ErrorCode.fromCode 返回 Optional | `ErrorCode.java` L49 | 返回 Optional 而非 null，调用方显式处理 |
| 39 | 枚举 fromValue/fromCode 返回 Optional | StatusEnum 等 4 个枚举 | 避免调用方 NPE |

### 3.3 定时任务增强

| # | 任务 | 涉及文件 | 操作 |
|---|------|---------|------|
| 40 | OverdueScanJob 加事务 | `OverdueScanJob.java` | 单条账单不可部分更新 |
| 41 | OverdueNoticeJob 增加发送记录 | `OverdueNoticeJob.java` | 记录发送状态，避免中断后重复发送 |
| 42 | MailService 增加重试机制 | `MailService.java` L42-L64 | Spring Retry，失败告警 |

---

# 第四阶段：🟢 前端体验 + 数据库规范

> 目标：提升用户体验、清理遗留脏数据、规范数据库结构
> 预估：2-3 天

---

### 4.1 前端优化

| # | 任务 | 涉及文件 | 操作 |
|---|------|---------|------|
| 43 | Token 存储改为 httpOnly Cookie | `request.ts`（admin + owner） | 后端 Set-Cookie httpOnly + 前端自动携带 |
| 44 | 401 改为路由跳转而非 window.location | `request.ts` | 使用 `router.push('/login')`，保留状态栈 |
| 45 | 路由守卫增加 Token 过期校验 | `router/index.ts` L59-L63 | 解析 JWT exp，提前踢出过期用户 |
| 46 | 添加 404 路由页面 | `router/index.ts` | catch-all 路由 → NotFound 组件 |
| 47 | 身份证号列表脱敏 | `OwnerList.vue` | 仅显示前 3 后 4 位，如 "320****1234" |
| 48 | 金额加千分位格式化 | 全前端 | `toLocaleString()` 或 Element Plus 格式化 |
| 49 | 状态映射统一抽取 | BillList / BillDetail / PaymentView | 一处定义，多处引用 |
| 50 | 银行支付弹窗不被拦截 | `PaymentView.vue` L79-L83 | window.open 放在用户点击事件同步调用 |
| 51 | Vite 代理地址环境变量化 | `vite.config.ts` L15-L16 | `VITE_API_BASE_URL` |
| 52 | 全量下拉改为搜索下拉 | RoomList / OwnerList 等 | `el-select` + `remote` + `remote-method` |
| 53 | 全局注册图标优化 | `main.ts` | 按需引入图标，不用全量注册 |
| 54 | blob 响应增加业务错误校验 | Excel 导出逻辑 | 检查 Content-Type 是否为 json（错误响应） |

### 4.2 数据库规范

| # | 任务 | 涉及位置 | 操作 |
|---|------|---------|------|
| 55 | 清理测试脏数据 | property_management.sql | 删除物业费666、room_type=0、空数据单元等 |
| 56 | t_bill.status 注释补全 | 字段注释 | 补充状态值 5 的说明 |
| 57 | t_bill 异常数据修正 | BILL202607020010 | 修正 due_date 使其 >= bill_date |
| 58 | t_payment 异常数据修正 | 多条 | payer_name 纠正为真实姓名，修正"已作废"状态数据 |
| 59 | t_sys_file 审计字段统一 | 表结构 | upload_by/upload_time → create_by/create_time |
| 60 | t_stats_arrears 补充审计字段 | 表结构 | 添加 del_flag, create_by, update_by, update_time |

---

# 第五阶段：🚀 生产就绪

> 目标：从"能跑"到"敢交付"，核心是资金链路可验证 + 一行命令可运行 + 出问题可发现
> 预估：4~7 天

> **设计原则**：不做"表演性"工作。不压测千级数据的系统，不搭 CI/CD 流水线，不全量上 Redis。
> 聚焦三个真实价值——"钱不出错、别人能跑、出事能查"。

---

### 5.1 🐳 Docker 容器化 —— 可运行的交付物

**为什么做**：面试官拿到仓库，一行命令看到活的系统，比任何 PPT 都有说服力。

**任务**：实现 `docker compose up` 一键启动全部服务

<details>
<summary><b>文件产出</b></summary>

```
├─ docker-compose.yml              # 编排所有服务
├─ Dockerfile                      # 后端 Spring Boot 多阶段构建（3个API共用）
├─ property-admin-web/Dockerfile   # 管理端前端 Nginx 镜像
├─ property-owner-web/Dockerfile   # 业主端前端 Nginx 镜像
└─ nginx/nginx.conf                # 前端统一入口，反向代理到后端
```
</details>

**服务编排清单**：

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| mysql | mysql:8.0 | 3306 | 数据持久化 volume，启动时自动执行 `sql/property_management.sql` |
| admin-api | 自构建 | 8081 | 管理端 API |
| owner-api | 自构建 | 8082 | 业主端 API |
| task | 自构建 | 8083 | 定时任务执行器 |
| admin-web | nginx:alpine | 80 | 管理端前端（含 API 反向代理） |
| owner-web | nginx:alpine | 81 | 业主端前端（含 API 反向代理） |
| xxl-job-admin | 官方jar | 9099 | 任务调度中心 |

> 不单独容器化 Redis——数据量和并发量不构成使用 Redis 的理由。

**Dockerfile 多阶段构建要点**（后端）：

```dockerfile
# 阶段1: Maven 编译
FROM maven:3.9-eclipse-temurin-21 AS builder
COPY . /src
WORKDIR /src
RUN mvn clean package -pl property-admin-api -am -DskipTests

# 阶段2: 运行镜像（瘦身）
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /src/property-admin-api/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

**关键配置项**（`docker-compose.yml`）：
- 健康检查：`healthcheck` 确保服务启动顺序（MySQL → API → Nginx）
- 环境变量：通过 `.env` 文件注入，不含真实密钥
- 网络隔离：前端容器只暴露 80/81 端口，后端 API 通过内部网络通信

**验证**：`docker compose up -d` → 浏览器 `http://localhost` 可登录管理端 → 能走通一次完整支付流程

**预估**：1.5~2 天

---

### 5.2 🧪 资金链路集成测试 —— 证明"钱不会出错"

**为什么做**：支付是唯一不可出错的功能。不是追求覆盖率，而是证明最危险的场景都被覆盖了。

**核心思想**：用集成测试 mock 支付宝 SDK，模拟支付链路上的异常路径。

#### 5.2.1 测试基础设施搭建

**文件产出**：

```
├─ property-module-payment/pom.xml                  # 加 spring-boot-starter-test + H2
├─ property-module-bill/pom.xml                     # 同上
└─ property-module-payment/src/test/resources/
   └─ application-test.yml                          # H2 内存数据库 + Mock支付宝Bean
```

**技术选型**：
- 数据库：H2 内存数据库（MySQL 兼容模式），不需真实 MySQL
- Mock：`@MockBean` 替换 `AlipayClient`，避免真实网络调用
- 框架：`@SpringBootTest` + JUnit 5 + AssertJ

#### 5.2.2 P0 级测试场景（资金安全，必须覆盖）

| # | 测试方法 | 场景描述 | 为什么重要 |
|---|---------|---------|-----------|
| T61 | `shouldGenerateOrderOnlyOnce` | 同一账单重复请求创建支付 → 幂等，不重复生成订单 | **防重复收款** |
| T62 | `shouldLockBillDuringOfflinePay` | 两个操作员同时对同一账单发起线下缴费 → 第二个被阻塞 | **防并发超收** |
| T63 | `shouldHandleDuplicateNotify` | 支付宝发送重复回调通知 → 只处理一次，不重复入账 | **防重复回调** |
| T64 | `shouldMarkOrderAsFailedWhenNotifyTimeout` | 支付宝未回调（模拟超时）→ 订单状态为待支付，可重新发起 | **防订单卡死** |
| T65 | `shouldReconcileMissedPayment` | 支付成功但回调丢失 → 对账任务发现并补录 | **防回调丢失** |
| T66 | `shouldRejectTamperedNotifySign` | 回调签名伪造 → 拒绝处理，不更新任何状态 | **防伪造回调** |

#### 5.2.3 P1 级测试场景（业务正确性）

| # | 测试方法 | 场景描述 |
|---|---------|---------|
| T67 | `shouldNotGenerateBillForPaidRoom` | 已缴费账单不重复生成 |
| T68 | `shouldNotGenerateBillForVoidedRoom` | 已作废账单不重复生成 |
| T69 | `shouldNotGenerateBillForDiscountedRoom` | 已减免账单不重复生成 |
| T70 | `shouldSetCorrectDueDate` | 账单截止日 = 出账日 + 收费周期，不早于出账日 |

**验证**：`mvn test -pl property-module-payment -am` 全部通过，6个P0必须全绿

**预估**：2~3 天（基础设施搭建 0.5 天 + P0 场景 1.5 天 + P1 场景 0.5 天）

---

### 5.3 🏥 生产可用性 —— 出事能发现

**为什么做**：这些不花什么时间，但面试时是区分"学生项目"和"交付项目"的关键信号。

#### 5.3.1 健康检查 + 优雅停机

| # | 任务 | 操作 | 预估 |
|---|------|------|------|
| T71 | 开启 Actuator 健康检查 | `pom.xml` 加 `spring-boot-starter-actuator`，配置 `management.endpoints.web.exposure.include=health` | 15min |
| T72 | 优雅停机 | `application.yml` 加 `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s` | 10min |
| T73 | 就绪探针 | Dockerfile 中 `HEALTHCHECK --interval=10s CMD curl -f http://localhost:8081/actuator/health \|\| exit 1` | 15min |
| T74 | MySQL 连接池健康 | HikariCP 已有，确认 `connection-test-query=SELECT 1` 生效 | 5min |

#### 5.3.2 结构化日志

| # | 任务 | 操作 | 预估 |
|---|------|------|------|
| T75 | JSON 格式日志（生产环境） | `logback-spring.xml`，dev profile 保留可读格式，prod profile 输出 JSON | 30min |
| T76 | 请求 traceId 串联 | `MDC.put("traceId", UUID)` 在 Filter 入口注入，响应头返回 `X-Trace-Id`，日志 pattern 打印 `%X{traceId}` | 30min |

**验证**：`ctrl+c` 后 30 秒内处理完进行中的请求 → 新请求直接拒绝 → 进程退出

**预估**：0.5 天

---

### 5.4 🚀 云服务器演示部署

**为什么做**：给面试官一个可访问的地址，附带一个能用支付宝沙箱跑通的演示账号。

> 前提：Docker 镜像构建完成（5.1）

**操作步骤**：

```
1. 购买阿里云/腾讯云轻量服务器 2C2G（~68元/月，或免费试用3个月）
2. 安装 Docker + Docker Compose
3. git clone 项目 + 配置 .env（填入真实的支付宝沙箱密钥）
4. docker compose up -d
5. 安全组开放 80, 81, 8081 端口
```

**演示准备**：

| 项 | 内容 |
|---|------|
| 演示地址 | `http://<公网IP>`（管理端）/ `http://<公网IP>:81`（业主端） |
| 管理端账号 | admin / admin123 |
| 业主端账号 | 13900001111 / 123456 |
| 演示流程 | 登录管理端 → 生成账单 → 业主端缴费（调支付宝沙箱）→ 管理端查看入账 |

**预估**：0.5 天（前提：Docker 已完成）

---

### 5.5 �️ 不做（及替代方案）

| 原计划内容 | 不做原因 | 替代方案 |
|-----------|---------|---------|
| Redis 全套缓存 | 30条配置数据用内存 Map 就够，引入 Redis 增加运维负担 | `@Cacheable` + Caffeine 本地缓存（零依赖，20分钟搞定） |
| JMeter 压测 | 千级数据压出TPS数字，面试官一眼看穿 | 改为在测试中验证 N+1 优化效果：`assertThat(sqlCount).isLessThan(5)` |
| CI/CD (GitHub Actions) | 项目在 Gitee，一个人开发用不上 | 如果迁移到 GitHub，写一个 `.github/workflows/ci.yml` 就行，不费时间 |
| JWT 黑名单用 Redis | 过度设计 | 缩短 JWT 有效期即可；真要登出，前端删 token，后端不做额外处理 |

---

### 5.6 📦 可选速赢项

如果还有余力，以下改动性价比极高（每项 ≤ 30 分钟）：

| 项 | 操作 | 信号 |
|----|------|------|
| API 版本号 | 所有 Controller 路径加 `/api/v1/` 前缀 | "我知道 API 需要版本管理" |
| 全局 CORS 配置 | `WebMvcConfigurer.addCorsMappings` 显式声明允许的域名 | "我知道跨域安全" |
| SQL 初始化脚本自动执行 | `docker-compose.yml` 中 MySQL 容器挂载 `sql/init/` 目录 | "我知道数据库需要自动化初始化" |

---

## 附录：第五阶段执行顺序

```
5.1 Docker（最优先）
  │  └─ 产出可运行的容器镜像
  │
  ├──→ 5.4 云部署（依赖 Docker）
  │
  ├──→ 5.2 集成测试（可与 Docker 并行，Mock 测试不依赖容器）
  │
  └──→ 5.3 生产可用性（穿插在 Docker 和测试间隙，碎片时间完成）
```

---

## 附录：执行顺序依赖图

```
Phase 1 (安全+Bug)
  │
  ├── 1.1 安全基础设施 ───────────────────────────── 无依赖，可最先开始
  │
  └── 1.2 关键Bug修复 ──── 部分依赖 1.1（如密钥变量）
        │
        v
Phase 2 (正确性+性能)
  │
  ├── 2.1 业务逻辑修复 ──── 依赖 1.2（如支付ID修复后才能测性能）
  │
  └── 2.2 性能优化 ────── 依赖 2.1（Bug修复后再优化）
        │
        v
Phase 3 (代码质量) ────── 依赖 Phase 2（不重复优化即将删除的代码）
        │
        v
Phase 4 (前端+数据库) ──── 可部分与 Phase 3 并行
        │
        v
Phase 5 (生产就绪)
  │
  ├── 5.1 Docker ──────── 依赖全部修复完成，优先执行
  │     │
  │     ├──→ 5.4 云部署 ──── 依赖 Docker 镜像
  │     │
  │     └──→ 5.2 集成测试 ── 可与 Docker 并行
  │           │
  │           └──→ 5.3 生产可用性 ── 穿插碎片时间完成
```

---

## 建议执行节奏

| 周次 | 阶段 | 产出 |
|------|------|------|
| 第 1 周 | Phase 1 + 2 | 所有安全漏洞修复完毕，核心 Bug 消灭，性能优化完成 |
| 第 2 周 | Phase 3 + 4 | 代码整洁统一，数据库规范，前端体验提升 |
| 第 3 周 | Phase 5（前半） | Docker 可运行，云部署可访问，集成测试基础设施搭建 |
| 第 4 周 | Phase 5（后半） | P0 级资金链路测试全绿，健康检查 + 结构化日志就绪 |

> 如果时间紧张，可压缩至 **3 周** 完成（Phase 1-4 两周 + Phase 5 一周，砍掉 P1 测试和可选速赢项）。

---

**执行计划完毕。**
