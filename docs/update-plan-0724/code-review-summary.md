# 物业管理信息系统 — 项目审查总结报告

> **审查日期**：2026-07-24
> **技术栈**：Spring Boot 3.2 + JDK 21 + MyBatis-Plus 3.5.7 / Vue 3 + TypeScript + Element Plus
> **审查范围**：全部 11 个 Maven 模块 + 2 个前端工程 + 数据库脚本 + 配置文件

---

## 目录

1. [🔴 严重安全风险（8项）](#1-严重安全风险)
2. [🟠 数据完整性 & 正确性风险（12项）](#2-数据完整性--正确性风险)
3. [🟡 性能问题（6项）](#3-性能问题)
4. [🟡 设计 & 代码质量问题（10项）](#4-设计--代码质量问题)
5. [🔵 数据库设计问题（6项）](#5-数据库设计问题)
6. [🟢 前端用户体验问题（5项）](#6-前端用户体验问题)
7. [📊 问题严重度分布](#7-问题严重度分布)

---

<a name="1-严重安全风险"></a>
## 1. 🔴 严重安全风险（8项）

| # | 问题描述 | 涉及文件 | 行号 | 影响 |
|---|---------|---------|------|------|
| 1 | **数据库密码明文硬编码** —— property-owner-api 和 xxl-job-admin 中将数据库用户名密码以明文写在 YAML 中，而其他模块使用环境变量注入，规范不统一 | `property-owner-api/src/main/resources/application.yml` | L9-L10 | 源码泄露即数据库失陷 |
| 2 | **JWT 密钥硬编码且管理端/业主端共享** —— 两个 API 模块使用完全相同的 Base64 密钥，且 `@Value` 有默认值兜底，任一模块泄露即影响全部 | `property-admin-api/src/main/resources/application.yml` L52-L53, `JwtUtil.java` L25 | 多处 | Token 伪造可跨系统生效 |
| 3 | **支付宝商户私钥存在 classpath 配置文件中** —— `@PropertySource("classpath:alipay-sandbox.properties")` 将商户私钥置于源码仓库，极易因版本控制/构建产物泄露 | `AlipayConfig.java` | L21-L34 | 私钥泄露可伪造支付请求 |
| 4 | **邮件 SMTP 密码明文硬编码** —— QQ邮箱授权码以明文写在 property-task 的配置中 | `property-task/src/main/resources/application.yml` | L11 | 邮箱失陷可发送钓鱼邮件 |
| 5 | **所有环境的 Swagger/Knife4j 开启** —— 未按 profile 区分，生产环境仍暴露完整 API 文档，包括接口参数、模型结构 | `property-admin-api/application.yml` | L26-L39 | 攻击者可获取完整接口信息 |
| 6 | **SQL 日志在生产环境输出到 stdout** —— `StdOutImpl` 将所有 SQL 语句及参数打印到标准输出 | `application.yml`（三个 API 模块） | L23-L24 | 敏感数据（身份证、金额）泄露 |
| 7 | **前端 Token 存储在 localStorage** —— XSS 攻击可窃取 Token，且无法设置 httpOnly/secure 标志 | `property-admin-web/src/utils/request.ts` | 全部 | Token 劫持后账号失陷 |
| 8 | **登录接口无暴力破解防护** —— 无验证码、无登录频率限制、无失败锁定机制 | `AdminAuthController.java` | 全部 | 可被字典攻击爆破密码 |

---

<a name="2-数据完整性--正确性风险"></a>
## 2. 🟠 数据完整性 & 正确性风险（12项）

| # | 问题描述 | 涉及文件 | 行号 | 影响 |
|---|---------|---------|------|------|
| 9 | **支付方式名称映射数组跨模块不一致** —— Bill 模块数组索引1="微信"、Payment 模块索引1="支付宝"，使用相同 method=1 在不同模块显示不同名称 | `PaymentOrderServiceImpl.java`（Bill 模块） | L28 | 支付方式名称显示完全错乱 |
| 10 | **manualPayment() 返回错误的支付记录ID** —— insertPayment 时写入数据库的 ID 与返回的 ID 不同（重新调用 IdWorker.getId()） | `BillServiceImpl.java` | L338-L363 | 调用方拿到 ID 后查不到记录 |
| 11 | **线下缴费存在 TOCTOU 竞态条件** —— `createPayOrder()` 未使用 `SELECT ... FOR UPDATE` 锁定账单行，并发下两个管理员可基于过期的 `paidSoFar` 计算待缴金额，导致超收 | `PaymentOrderService.java` | L46-L115 | 并发场景下多收费 |
| 12 | **支付宝支付时间解析失败时静默使用当前时间** —— 使用当前时间冒充真实支付时间，影响财务对账和统计报表 | `PaymentCallbackTxService.java`, `PaymentReconciliationTxService.java` | L126-L133 | 对账数据不准确 |
| 13 | **账单生成幂等检查未排除"已减免"状态** —— 仅排除了 VOIDED(3) 状态，DISCOUNTED(4) 状态会被重复生成 | `BillServiceImpl.java` | L120-L129 | 已减免账单被重新生成 |
| 14 | **业主身份证号在列表页以明文展示** —— 未做脱敏处理，违反隐私数据保护最佳实践 | `OwnerList.vue` | 全部 | 隐私泄露风险 |
| 15 | **update() 中 idCardNo/phone 空指针风险** —— `entity.getIdCardNo().equals(...)` 在字段为 null 时抛出 NPE | `OwnerServiceImpl.java` | L124-L126 | 部分业主无法更新 |
| 16 | **OwnerEntity 的 password 字段缺少 @JsonIgnore** —— 密码哈希可能在序列化时泄露给前端 | `OwnerEntity.java` | L22-L23 | 密码哈希泄露 |
| 17 | **insertPayment SQL 中 create_by 硬编码 'admin'** —— 覆盖了 MyBatis-Plus 自动填充，丢失真实操作人 | `BillPaymentMapper.java` | L29 | 审计信息不准确 |
| 18 | **t_payment.payer_name 存储了"业主ID:4002"格式的数据** —— 代码中将 owner_id 拼接存入，非真实业主姓名 | `property_management.sql` | L889-L910 | 支付记录查询展示异常 |
| 19 | **t_bill 存在 due_date 早于 bill_date 的数据** —— 缴费截止日期早于出账日期，逻辑不合理 | `property_management.sql` | L76 | 数据逻辑错误 |
| 20 | **前端作废账单调用了 manualPayment（缴费）接口** —— 语义和逻辑完全错误 | `BillList.vue` | 多处 | 功能误用 |

---

<a name="3-性能问题"></a>
## 3. 🟡 性能问题（6项）

| # | 问题描述 | 涉及位置 | 严重度 |
|---|---------|---------|--------|
| 21 | **N+1 查询（共 3 处）** —— 分页查询后对每条记录循环调用独立 SQL 获取楼栋名称、业主姓名等关联数据，每页 20 条产生 10~40 次额外查询 | BillServiceImpl、OwnerRoomServiceImpl、PaymentOrderServiceImpl 的 toVO() 方法 | 高 |
| 22 | **账单编号使用 LIKE 模糊查询生成流水号** —— `likeRight('BILL20260724%')` 无法利用前缀索引，数据量增长后性能急剧下降 | `BillServiceImpl.java` L265-L284, `PaymentOrderService.java` L138-L151 | 高 |
| 23 | **支付单号生成并发不安全** —— 先查询当日最大单号再 +1，高并发下两个线程读到相同值产生重复单号 | `PaymentOrderService.java` L138-L151 | 中 |
| 24 | **所有表缺少 del_flag 索引** —— MyBatis-Plus 所有查询附加 `WHERE del_flag=0` 条件，可能导致全表扫描 | 全部数据库表 | 中 |
| 25 | **报告导出全量加载到内存再写入** —— 大量数据场景下 OOM | `ReportExportService.java` | 中 |
| 26 | **前端全量加载下拉选项（size=9999）** —— 房屋/业主/单元下拉框一次性加载全部数据 | `RoomList.vue` 等多处 | 低 |

---

<a name="4-设计--代码质量问题"></a>
## 4. 🟡 设计 & 代码质量问题（10项）

| # | 问题描述 | 涉及位置 | 类别 |
|---|---------|---------|------|
| 27 | **停车位对账无去重机制** —— 每次运行直接 INSERT 预警，快速重复插入导致预警表膨胀 | `ParkingReconciliationServiceImpl.java` L48-L74 | 逻辑缺陷 |
| 28 | **Excel 导入无事务** —— 中间行失败不回滚已成功导入的行 | `OwnerExcelService.java` | 事务缺失 |
| 29 | **ResponseAdvice 对 String 返回值有 ClassCastException 风险** —— `supports()` 跳过 String 但 `beforeBodyWrite` 仍可能收到 String | `ResponseAdvice.java` L43 | 潜在 Bug |
| 30 | **SysConfigService 每次直接查询数据库** —— 无缓存层，高频读取场景性能差 | `SysConfigService.java` L71-L78 | 性能 |
| 31 | **MySQL GET_LOCK 命名锁在连接池下可能失效** —— 进程崩溃导致锁无法自动释放 | `PaymentCallbackService.java` L77-L91 | 设计缺陷 |
| 32 | **PaymentCallbackTxService 和 PaymentReconciliationTxService 大量重复代码** —— parsePaymentTime、markAsFailedInTx 等方法代码完全一致 | 两个 TxService | 代码冗余 |
| 33 | **前端多处空 catch 块吞掉错误** —— 无日志输出，线上问题难以排查 | admin-web 和 owner-web 多视图 | 可维护性 |
| 34 | **路由守卫不验证 token 时效性** —— 仅检查 token 是否存在，过期 token 也能访问页面直到 API 返回 401 | `router/index.ts` L59-L63 | 用户体验 |
| 35 | **Vite 代理地址硬编码** —— 无环境变量支持，切换环境需修改源码 | `vite.config.ts` L15-L16 | 可维护性 |
| 36 | **配置管理规范不统一** —— 部分使用 `${}` 环境变量，部分直接明文硬编码 | 多个 application.yml | 安全 |

---

<a name="5-数据库设计问题"></a>
## 5. 🔵 数据库设计问题（6项）

| # | 问题描述 | 涉及位置 |
|---|---------|---------|
| 37 | **所有表缺少外键约束** —— 表之间存在明确的关联关系（如 bill.room_id → room.id）但未定义任何 FOREIGN KEY | 全部表 |
| 38 | **t_sys_file 审计字段命名不一致** —— 使用 upload_by/upload_time 而非通用的 create_by/create_time | `property_management.sql` L1102-L1115 |
| 39 | **t_stats_arrears 缺少 del_flag 和标准审计字段** —— 与其他表结构规范不一致 | `property_management.sql` L1003-L1016 |
| 40 | **测试数据残留** —— t_fee_item 有 "物业费666"（拼写错误）、t_room 有 room_type=0（定义范围1-5）、t_unit 有空值数据 | SQL 脚本多处 |
| 41 | **t_bill.status 注释缺少状态值5** —— 注释仅列出 0-4，但实际数据中存在 status=5 | `property_management.sql` L43 |
| 42 | **t_owner_room.is_primary 缺少唯一约束** —— 同一房屋可能被设置多个"主要业主" | `property_management.sql` L601-L616 |

---

<a name="6-前端用户体验问题"></a>
## 6. 🟢 前端用户体验问题（5项）

| # | 问题描述 | 涉及位置 |
|---|---------|---------|
| 43 | **401 响应时 window.location.href 硬跳转** —— 丢失全部 Pinia store 状态，未调用退出登录接口 | `request.ts` |
| 44 | **支付弹窗可能被浏览器拦截** —— window.open 在 await 之后执行，浏览器认为是非用户触发操作 | `PaymentView.vue` L79-L83 |
| 45 | **金额缺少千分位格式化** —— 大数字时用户可读性差 | 管理员端和业主端多页面 |
| 46 | **路由缺少 404 页面** —— 不存在的路径显示空白 | `router/index.ts` |
| 47 | **状态枚举/映射散落在各视图重复定义** —— BillList、BillDetail、PaymentView 各自定义 statusMap | 业主前端多页面 |

---

<a name="7-问题严重度分布"></a>
## 7. 问题严重度分布

```
严重度      数量      占比
─────────────────────────────
🔴 严重       8        ~19%
🟠 高        12        ~29%
🟡 中        22        ~52%
─────────────────────────────
总计         42+

按模块分布：
├─ 后端基础设施 (common+framework)     ~18项
├─ 后端业务模块 (bill+owner+parking+)   ~26项
├─ 后端 API 层 (admin-api+owner-api)   ~25项
├─ 前端 admin-web                      ~29项
├─ 前端 owner-web                      ~18项
├─ 数据库 & 配置                       ~29项
```

---

> **说明**：本报告仅为问题汇总，未实施任何修复。各问题的详细代码定位和修复建议见上方表格中的文件链接。如有需要可以针对具体问题展开讨论。
