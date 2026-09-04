# 物业管理系统 — 优化实施记录

> 记录日期：2026-08-07
> 已完成阶段：第一阶段（安全修复 + 关键 Bug）、第二阶段（数据正确性 + 性能优化）

---

## 第一阶段：安全修复 + 关键 Bug

### 安全基础设施（9 项）

| # | 任务 | 涉及文件 | 修改内容 |
|---|------|---------|---------|
| S1 | 数据库密码环境变量化 | `property-owner-api/application.yml` | `username`/`password` → `${MYSQL_USER}` / `${MYSQL_PASSWORD}` |
| S1 | XXL-Job 数据库密码 | `xxl-job-admin/application-prod.yml` | `username`/`password`/`url`/`log path` → 环境变量 |
| S2 | JWT Secret 区分管理端/业主端 | `JwtUtil.java` | 移除 `@Value` 默认值，启动时必须配置 |
| S2 | 管理端 JWT 密钥 | `property-admin-api/application.yml` | `jwt.secret` → `${JWT_ADMIN_SECRET}` |
| S2 | 业主端 JWT 密钥 | `property-owner-api/application.yml` | `jwt.secret` → `${JWT_OWNER_SECRET}`（与管理端密钥分离） |
| S3 | 支付宝私钥移出源码 | `AlipayConfig.java` | 移除 `@PropertySource("classpath:alipay-sandbox.properties")`，改为环境变量注入 |
| S4 | 邮件密码环境变量化 | `property-task/application.yml` | `mail.password` → `${MAIL_PASSWORD}`，`mail.username` → `${MAIL_USERNAME}` |
| S5 | XXL-Job 调度配置环境变量化 | `property-task/application.yml` | `admin.addresses`、`executor.ip`、`executor.logpath`、`accessToken` 全部改为环境变量 |
| S6 | Swagger 仅开发环境开启 | `property-admin-api/application.yml`、`property-owner-api/application.yml` | `springdoc`/`knife4j` 默认关闭，通过 `${SWAGGER_ENABLED:false}` 控制 |
| S6 | SQL 日志默认关闭 | 3 个 `application.yml` | `log-impl` 默认值改为 `NoLoggingImpl`，通过 `${MYBATIS_LOG_IMPL}` 控制 |

### 关键数据正确性 Bug（4 项）

| # | 任务 | 涉及文件 | 修改内容 |
|---|------|---------|---------|
| B1 | 支付方式名称映射修复 | `PaymentOrderServiceImpl.java` (bill 模块) | 数组从 `{"","微信","支付宝",...}` → `{"","支付宝","微信",...}`，与 Payment 模块一致 |
| B1 | 账单幂等检查补全 | `BillServiceImpl.java` | `generateBillForRoom()` 排除所有终态：VOIDED + DISCOUNTED + PAID |
| B2 | manualPayment 返回错误 ID | `BillServiceImpl.java` | `manualPayment()`/`itemizedPayment()` 保存 `IdWorker.getId()` 后复用，不再生成第二个 ID |
| B3 | 线下缴费并发超收 | `BillMapper.java` | 新增 `selectByIdForUpdate()` 行锁查询 |
| B3 | 线下缴费并发超收 | `PaymentOrderService.java` (payment 模块) | 线下支付路径使用 `SELECT FOR UPDATE` 锁定账单行 |

### 额外修复

| 任务 | 涉及文件 | 修改内容 |
|------|---------|---------|
| parsePaymentTime 警告日志 | `PaymentCallbackTxService.java`、`PaymentReconciliationTxService.java` | 解析失败时记录 `log.warn` |

### 新增文件

| 文件 | 说明 |
|------|------|
| `.env.example` | 环境变量配置模板，包含所有变量的说明和默认值 |

---

## 第二阶段：数据正确性 + 性能优化

### 业务逻辑修复（7 项）

| # | 任务 | 涉及文件 | 修改内容 |
|---|------|---------|---------|
| B4 | insertPayment 移除硬编码 admin | `BillPaymentMapper.java` | SQL 中 `'admin'` → `#{createBy}` 参数 |
| B4 | 调用方传入 createBy | `BillServiceImpl.java` | `manualPayment()`/`itemizedPayment()` 传入 `SecurityUtil.getUsername()` |
| B4 | 支付模块调用方补参 | `PaymentOrderService.java` (payment 模块) | 同样补上 `createBy` 参数，fallback 为 `"system"` |
| B5 | OwnerEntity 密码防序列化 | `OwnerEntity.java` | `password` 字段添加 `@JsonIgnore` |
| B6 | OwnerServiceImpl NPE 修复 | `OwnerServiceImpl.java` | `entity.getIdCardNo().equals()` → `Objects.equals()` |
| B7 | 默认密码安全化 | `OwnerServiceImpl.java` | `phone.substring(5)` → `SecureRandom` 生成6位随机数 + 手机号长度校验 |
| — | 添加 @Slf4j | `OwnerServiceImpl.java` | 补充 `@Slf4j` 注解和 import |

### 方法语义重命名（3 处）

| 文件 | 修改前 | 修改后 | 原因 |
|------|--------|--------|------|
| `BillPaymentMapper.java` | `updatePaymentSuccess` | `updatePaymentStatus` | 方法处理所有状态转换，非仅 SUCCESS |
| `PaymentCallbackTxService.java` | `updatePaymentStatusOnly` | `markPaymentNonSuccess` | 明确语义：标记非成功状态 |
| `PaymentReconciliationTxService.java` | `updatePaymentSuccess` (3处) | `updatePaymentStatus` | 同步 Mapper 方法重命名 |

### 车位对账优化（2 项）

| # | 任务 | 涉及文件 | 修改内容 |
|---|------|---------|---------|
| B9 | 预警去重 | `ParkingWarningMapper.java` | 新增 `countActiveBySpaceAndType()` 查询 |
| B9 | 预警去重 + 事务拆分 | `ParkingReconciliationServiceImpl.java` | 5个check方法加 `isDuplicate()` 跳过已有预警；移除 `reconcile()` 外层事务，每项独立提交 |

### 性能优化（3 项）

| # | 优化项 | 涉及文件 | 修改内容 | 效果 |
|---|--------|---------|---------|------|
| P1 | 账单分页 N+1 | `BillRoomMapper.java` | 新增 `selectRoomBuildingInfoBatch()` | — |
| P1 | 账单分页 N+1 | `BillServiceImpl.java` | 新增 `fillBuildingInfo()` 批量查询 | 40次 → 3次 SQL |
| P2 | 支付记录分页 N+1 | `BillOwnerRoomMapper.java` | 新增 `selectOwnerInfoBatch()` | — |
| P2 | 支付记录分页 N+1 | `BillRoomMapper.java` | 新增 `selectRoomCodeNameBatch()` | — |
| P2 | 支付记录分页 N+1 | `PaymentOrderServiceImpl.java` (bill 模块) | 分页后批量预查房屋/业主/账单 | 120+次 → 5-6次 SQL |
| P3 | 系统配置缓存 | `MyBatisPlusConfig.java` | 添加 `@EnableCaching` | — |
| P3 | 系统配置缓存 | `SysConfigService.java` | `getConfig()` 加 `@Cacheable`；新增 `refreshCache()` 手动失效 | 首次查库后走缓存 |

---

## 累计统计

| 阶段 | 修改文件数 | 新增文件数 | 主要成果 |
|------|-----------|-----------|---------|
| 第一阶段 | 12 | 1 (.env.example) | 消除所有硬编码密钥/密码；修复 3 个资金相关 Bug |
| 第二阶段 | 13 | 0 | 修复 5 个业务正确性问题；3 项 N+1 性能优化 |
| **合计** | **25** | **1** | — |

### 跳过的任务

| 编号 | 任务 | 原因 |
|------|------|------|
| B8 | ParkingSpaceServiceImpl 外键校验 | `property-module-parking` 不依赖 owner/bill 模块，需调整模块依赖关系 |

### 环境变量总览

| 变量名 | 用途 | 使用模块 |
|--------|------|---------|
| `MYSQL_HOST` | MySQL 主机 | 所有模块 |
| `MYSQL_USER` | 数据库用户名 | admin-api, owner-api, task |
| `MYSQL_PASSWORD` | 数据库密码 | admin-api, owner-api, task |
| `XXL_MYSQL_USER` | XXL-Job 数据库用户名 | xxl-job-admin |
| `XXL_MYSQL_PASSWORD` | XXL-Job 数据库密码 | xxl-job-admin |
| `JWT_ADMIN_SECRET` | 管理端 JWT 密钥 | admin-api |
| `JWT_OWNER_SECRET` | 业主端 JWT 密钥 | owner-api |
| `JWT_EXPIRATION` | JWT 有效期 (ms) | admin-api, owner-api |
| `ALIPAY_APP_ID` ~ `ALIPAY_RETURN_URL` | 支付宝沙箱配置 | payment 模块 |
| `MAIL_USERNAME` | 邮箱账号 | task |
| `MAIL_PASSWORD` | 邮箱 SMTP 授权码 | task |
| `XXL_JOB_ADMIN_ADDRESSES` | 调度中心地址 | task |
| `XXL_JOB_EXECUTOR_IP` | 执行器 IP | task |
| `XXL_JOB_ACCESS_TOKEN` | XXL-Job 访问令牌 | task, xxl-job-admin |
| `SWAGGER_ENABLED` | Swagger 开关 | admin-api, owner-api |
| `MYBATIS_LOG_IMPL` | SQL 日志实现类 | admin-api, owner-api, task |
| `LOG_PATH` | 日志输出路径 | task, xxl-job-admin |

---

## 第三阶段：代码质量 + 设计优化

> 实施日期：2026-08-07

### 消除代码冗余（2 项）

| # | 任务 | 涉及文件 | 修改内容 |
|---|------|---------|---------|
| 33 | 统一日志声明风格 | `BillServiceImpl.java` | `LoggerFactory.getLogger()` → `@Slf4j`，移除手动 Logger 声明和 import |
| 32 | 抽取 parsePaymentTime | `PaymentTimeUtil.java`（新建） | 抽取为工具类静态方法 |
| 32 | 消除 TxService 重复代码 | `PaymentCallbackTxService.java` | 删除私有 `parsePaymentTime` 方法，改为 `PaymentTimeUtil.parse()` |
| 32 | 消除 TxService 重复代码 | `PaymentReconciliationTxService.java` | 删除私有 `parsePaymentTime` 方法和 `DATE_FMT` 常量，改为调用工具类 |

### 设计缺陷修复（4 项）

| # | 任务 | 涉及文件 | 修改内容 |
|---|------|---------|---------|
| 35 | AlipayApiException→业务异常 | `AlipayService.java` | `createPagePay()`/`queryTrade()` 的 catch 从 `RuntimeException` → `BusinessException(ErrorCode.OPERATION_FAILED)` |
| 38 | ErrorCode.fromCode 返回 Optional | `ErrorCode.java` | 返回值类型 `ErrorCode` → `Optional<ErrorCode>`（无调用方，安全改） |
| 39 | 枚举 fromValueSafe | `StatusEnum.java` | 新增 `fromValueSafe()` 返回 `Optional<StatusEnum>` |
| 39 | 枚举 fromValueSafe | `BillStatusEnum.java`（property-common） | 新增 `fromValueSafe()` 返回 `Optional<BillStatusEnum>` |
| 39 | 枚举 fromValueSafe | `OwnerStatusEnum.java` | 新增 `fromValueSafe()` 返回 `Optional<OwnerStatusEnum>` |
| 39 | 枚举 fromValueSafe | `FeeTypeEnum.java` | 新增 `fromCodeSafe()` 返回 `Optional<FeeTypeEnum>` |
| 39 | 枚举 fromValueSafe | `PaymentStatusEnum.java` | 新增 `fromValueSafe()` 返回 `Optional<PaymentStatusEnum>` |

### 定时任务增强（1 项）

| # | 任务 | 涉及文件 | 修改内容 |
|---|------|---------|---------|
| 40 | OverdueScanJob 事务 | `OverdueScanJob.java` | `scan()` 添加 `@Transactional(rollbackFor = Exception.class)` |

### 跳过的任务（附原因）

| 编号 | 任务 | 原因 |
|------|------|------|
| 31 | 合并线下缴费重复实现 | 跨模块依赖（bill→payment），BillServiceImpl 在 bill 模块无法反向调用 PaymentOrderService |
| 34 | MySQL GET_LOCK 改为 Redis 分布式锁 | Redis 尚未引入，留待第五阶段（Docker+Redis）处理 |
| 36 | ResponseAdvice String 返回值安全处理 | `supports()` L43 已执行 `returnClass == String.class → return false`，无需修改 |
| 37 | JacksonConfig Long→String 精确控制 | 改动风险高：全局替换会破坏现有前端 ID 显示，需逐字段加 `@JsonSerialize`，留待后续评估 |
| 41 | OverdueNoticeJob 增加发送记录 | 需新建 `t_notice_send_log` 表，留待后续阶段 |
| 42 | MailService 增加重试机制 | 需引入 Spring Retry 依赖，留待后续阶段 |

---

## 累计统计

| 阶段 | 修改文件数 | 新增文件数 | 主要成果 |
|------|-----------|-----------|---------|
| 第一阶段 | 12 | 1 (.env.example) | 消除所有硬编码密钥/密码；修复 3 个资金相关 Bug |
| 第二阶段 | 13 | 0 | 修复 5 个业务正确性问题；3 项 N+1 性能优化 |
| 第三阶段 | 9 | 1 (PaymentTimeUtil.java) | 统一日志风格；异常处理规范化；枚举安全封装；人工对账去重 |
| 第四阶段（DB） | 1 (SQL) | 0 | 清理脏数据；状态注释补全；异常数据修正；审计字段补充 |
| **合计** | **35** | **2** | — |

---

## 第四阶段（数据库部分）：数据清理 + 结构规范

> 实施日期：2026-08-07
> 注意：需手动删库后重新导入 `property_management.sql`

### 数据清理（3 项）

| # | 任务 | 修改内容 |
|---|------|---------|
| T55 | 清理测试脏数据 | 删除 `t_fee_item` 中 `item_code='PROPERTY_FEEE'` 的"物业费666"记录 |
| T57 | due_date 异常修正 | `BILL202607020010` 的 `due_date` 从 `2026-06-15` → `2026-07-31`（修复截止日早于出账日） |
| T58 | payer_name 修正 | 13 条 `t_payment` 记录：`payer_name` 从 "业主ID:XXXX" → 真实姓名（基于 `owner_id` 映射）；`PAY202607020001` 的 `remark` 从 "已作废" → "前台现金缴纳" |

### 结构规范（3 项）

| # | 任务 | 表 | 修改内容 |
|---|------|-----|---------|
| T56 | status 注释补全 | `t_bill` | `status` 字段 COMMENT 追加 `5-已逾期` |
| T59 | 审计字段注释规范 | `t_sys_file` | `upload_by` COMMENT: 上传人→创建人；`upload_time` COMMENT: 上传时间→创建时间（字段名保留不改为兼容现有代码） |
| T60 | 审计字段补充 | `t_stats_arrears` | 新增 `del_flag`、`create_by`、`update_by`、`update_time` 四列 |

### 第4.1节（前端优化，未实施）

12 项前端优化任务（T43-T54）留待后续实施，详见 [execution-plan.md](execution-plan.md)。
