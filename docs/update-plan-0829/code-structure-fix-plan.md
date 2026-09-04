# 代码结构修复计划

> 日期：2026-09-29  
> 来源：项目代码结构审阅（22 项问题）  
> 原则：仅修复现有代码结构问题，不扩展业务功能

---

## 修复优先级说明

| 级别 | 标志 | 含义                                               |
| ---- | ---- | -------------------------------------------------- |
| P0   | 🔴   | 破坏分层架构 / 模块边界 / 数据一致性风险，必须修复 |
| P1   | 🟡   | 代码重复 / 风格不一致，建议修复                    |
| P2   | 🟢   | 低影响，可择机修复                                 |

---

## 阶段一：高严重度修复（P0，共 9 项）

### 1.1 Controller 直接操作 Mapper

**涉及文件**：OwnerBillController、OwnerPaymentController、OwnerProfileController

| 文件                     | 当前问题                                                                                  | 修复方案                                                                                             |
| ------------------------ | ----------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `OwnerBillController`    | 注入 `OwnerRoomMapper`，直接在 Controller 中 `selectList()` + `stream().map()` 查业主房屋 | 在 `OwnerAuthService` 中新增 `getOwnerRooms(Long ownerId)` 方法，Controller 仅调用该方法             |
| `OwnerPaymentController` | 注入 `BillMapper`，直接在 Controller 中查账单 + 归属校验 + 状态校验                       | 在 `PaymentOrderService` 中新增 `validateAndGetBill(Long billId, Long ownerId)` 方法，将校验逻辑下沉 |
| `OwnerProfileController` | 注入 `BillRoomMapper`，逐条查询房屋信息（N+1 查询）                                       | 在 `OwnerAuthService` 或新建 `OwnerRoomService` 中新增批量查询方法，一次性返回房屋列表               |

**验收标准**：以上 3 个 Controller 不再注入任何 Mapper，所有数据访问通过 Service 层。

---

### 1.2 Controller 包含业务逻辑

**涉及文件**：OwnerAuthController、OwnerPaymentController、OwnerBillController、ChatController、PaymentController

| 文件                                      | 当前问题                                                                                               | 修复方案                                                                                                                  |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------- |
| `OwnerAuthController.login()`             | 构建 `LoginUser`、签发 Token、写 Cookie（与 `AdminAuthController` 已委托给 `AdminAuthService` 不一致） | 参照 `AdminAuthService` 模式，新建 `OwnerAuthService` 的 `login()` 方法，将 Token 签发 + Cookie 写入逻辑迁移到 Service 层 |
| `OwnerAuthController.verifyCaptcha()`     | 验证码校验私有方法                                                                                     | 将 `verifyCaptcha` 逻辑移入 `OwnerAuthService`，Controller 仅调用                                                         |
| `OwnerPaymentController.createPayOrder()` | 支付方式校验、账单归属校验、状态校验（与 `PaymentOrderService.createPayOrder()` 重复）                 | 删除 Controller 中的重复校验，统一由 `PaymentOrderService` 处理                                                           |
| `OwnerBillController.getDetail()`         | 在 Controller 中校验账单是否属于当前业主                                                               | 将归属校验下沉到 `BillService.getDetail()` 中，传入 `ownerId` 参数                                                        |
| `ChatController.createSession()`          | 手动将 Entity 属性逐个 set 到 VO                                                                       | 在 `AiChatService` 中封装 `toVO(ChatSession entity)` 方法                                                                 |
| `PaymentController.alipayNotify()`        | 手动解析 `HttpServletRequest` 参数构建 Map                                                             | 在 `AlipayService` 中新增 `parseNotifyParams(HttpServletRequest)` 方法                                                    |

**验收标准**：Controller 方法体仅包含 Service 调用 + ApiResult 返回，无业务判断、无数据转换、无私有业务方法。

---

### 1.3 Service 直接操作 HttpServletResponse

**涉及文件**：AdminAuthServiceImpl、AuditLogStatService、PaymentOrderService

| 文件                                    | 当前问题                                                                        | 修复方案                                                                                                            |
| --------------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `AdminAuthServiceImpl`                  | `login/refresh/logout` 方法签名和实现中直接操作 `HttpServletResponse` 写 Cookie | Cookie 写入操作回退到 Controller 层。Service 层仅返回 Token 字符串，Controller 负责调用 `TokenCookieUtil` 写 Cookie |
| `AuditLogStatService.exportAuditLog()`  | 直接操作 `response.setContentType/setHeader/getOutputStream`                    | 改为返回 `byte[]` 或接受 `OutputStream` 参数，HTTP 头设置留在 Controller                                            |
| `PaymentOrderService.isMobileRequest()` | 通过 `RequestContextHolder` 获取 User-Agent 判断设备                            | 删除此方法，由 Controller 根据 `HttpServletRequest` 判断设备类型后调用不同的支付方法                                |

**验收标准**：Service 层不再出现 `HttpServletResponse`、`RequestContextHolder` 等 Web 层依赖。

---

### 1.4 Entity 放在 API 模块中

**涉及文件**：

- `property-admin-api/entity/BuildingEntity.java`
- `property-admin-api/entity/RoomEntity.java`
- `property-admin-api/entity/UnitEntity.java`
- `property-admin-api/entity/SysUserEntity.java`
- `property-admin-api/repository/BuildingMapper.java`
- `property-admin-api/repository/RoomMapper.java`
- `property-admin-api/repository/UnitMapper.java`
- `property-admin-api/repository/SysUserMapper.java`

**修复方案**：

1. 新建 `property-module-building` 模块，迁移 `BuildingEntity`、`BuildingMapper`、`BuildingService`、`BuildingServiceImpl`
2. 新建 `property-module-room` 模块，迁移 `RoomEntity`、`RoomMapper` 及 `UnitEntity`、`UnitMapper`、`RoomService`、`RoomServiceImpl`、`UnitService`、`UnitServiceImpl`
3. 新建 `property-module-system` 模块，迁移 `SysUserEntity`、`SysUserMapper`、`SysUserService`、`SysUserServiceImpl`
4. 更新 `MyBatisPlusConfig` 的 `@MapperScan` 配置，追加新模块路径
5. 更新 `admin-api` 和 `owner-api` 模块的 pom.xml 依赖

**验收标准**：`property-admin-api` 模块中不再包含 `entity/` 和 `repository/` 目录。

---

### 1.5 跨模块裸 SQL 访问

**涉及文件**：`BillRoomMapper`、`BillOwnerRoomMapper`

**修复方案**：

- `BillRoomMapper` 替代方案：调用 `property-module-room` 的 `RoomService` 获取房屋信息
- `BillOwnerRoomMapper` 替代方案：调用 `property-module-owner` 的 `OwnerRoomService` 获取业主房屋关系

**验收标准**：`property-module-bill` 中不再通过裸 SQL 直接访问 `t_room`、`t_building`、`t_owner_room`、`t_owner` 表。

---

### 1.6 BillStatusEnum 双重定义且值冲突

**涉及文件**：

- `property-common/enums/BillStatusEnum.java`（零引用，死代码）
- `property-module-bill/entity/BillStatusEnum.java`（实际使用）

**修复方案**：删除 `property-common` 中的 `BillStatusEnum.java`。

**验收标准**：项目中仅存在一个 `BillStatusEnum` 定义。

---

### 1.7 ROLE 常量零散定义

**涉及文件**：`AdminAuthServiceImpl`、`OwnerAuthController`、`SysUserServiceImpl`

**修复方案**：在 `property-common` 中新建 `RoleConstant` 常量类，统一定义 `ROLE_ADMIN`、`ROLE_OWNER`、`ROLE_FINANCE` 等角色常量，上述文件改为引用常量。

**验收标准**：项目中不再出现硬编码的角色字符串。

---

### 1.8 ACCESS_COOKIE / REFRESH_COOKIE 重复定义

**涉及文件**：`AdminAuthServiceImpl`、`OwnerAuthController`、`AuthInterceptor`

**修复方案**：将 Cookie 名称常量 `ACCESS_COOKIE`、`REFRESH_COOKIE` 统一迁移到 `TokenCookieUtil` 中，作为公共常量暴露。

**验收标准**：Cookie 名称常量仅定义在一处。

---

### 1.9 createBy 硬编码

**涉及文件**：`BuildingServiceImpl`

**修复方案**：`entity.setCreateBy("admin")` 改为 `SecurityUtil.getLoginUser().getUsername()`，与 `MetaObjectHandler` 的自动填充逻辑保持一致。

**验收标准**：`createBy` 和 `updateBy` 不再硬编码。

---

## 阶段二：中严重度修复（P1，共 8 项）

### 2.1 generatePaymentNo() 方法重复

**涉及文件**：`BillServiceImpl`、`PaymentOrderService`

**修复方案**：提取到 `property-common` 的 `PaymentNoGenerator` 工具类中，统一生成规则。

---

### 2.2 PAYMENT_METHOD_NAMES 在 3 处重复定义

**涉及文件**：`bill/PaymentOrderServiceImpl`、`payment/PaymentOrderService`、`admin/PaymentOrderExcelService`

**修复方案**：创建 `PaymentMethodEnum` 枚举，替代字符串数组，统一管理支付方式名称映射。

---

### 2.3 toLong() 工具方法重复

**涉及文件**：`DashboardServiceImpl`、`ParkingReconciliationServiceImpl`

**修复方案**：提取到 `property-common` 的 `NumberUtils` 工具类中。

---

### 2.4 PaymentOrderService 同名类冲突

**涉及文件**：

- `property-module-bill/service/PaymentOrderService.java`（接口，负责查询）
- `property-module-payment/service/PaymentOrderService.java`（具体类，负责支付下单）

**修复方案**：将 bill 模块的 `PaymentOrderService` 重命名为 `PaymentOrderQueryService`，避免同名混淆。

---

### 2.5 @Autowired 与 @RequiredArgsConstructor 混用

**涉及文件**：`PaymentController`、`AdminPaymentController`

**修复方案**：统一使用 `@RequiredArgsConstructor`（构造器注入），将 `@Autowired(required = false)` 的可选依赖改为通过 `Optional` 或条件注入处理。

---

### 2.6 Token 刷新失败处理不一致

**涉及文件**：`AdminAuthServiceImpl.refresh()`、`OwnerAuthController.refresh()`

**修复方案**：统一为抛出 `BusinessException(ErrorCode.UNAUTHORIZED, ...)`，由全局异常处理器统一返回 401。

---

### 2.7 OperationLogQuery 分页字段名不一致

**涉及文件**：`OperationLogQuery.java`

**修复方案**：`OperationLogQuery` 继承 `PageQuery`，使用 `current`/`size`，同步修改前端和 `AdminStatisticController`。

---

### 2.8 AdminWebMvcConfig 与 OwnerWebMvcConfig 高度重复

**涉及文件**：`AdminWebMvcConfig`、`OwnerWebMvcConfig`

**修复方案**：在 `property-framework` 中创建 `BaseWebMvcConfig` 抽象类，通过构造参数传入路径模式，两个子类仅配置路径即可。

---

## 阶段三：低严重度修复（P2，共 5 项）

### 3.1 ImportResult 内嵌类重复定义

**涉及文件**：`AdminOwnerExcelController`、`AdminPaymentController`

**修复方案**：提取为公共泛型类 `ImportResult<T>`，放在 `property-common/dto` 下。

---

### 3.2 日志声明方式不一致

**涉及文件**：`AuthInterceptor`、`SysUserServiceImpl`、`GlobalExceptionHandler`、`OwnerImportListener`

**修复方案**：将 `LoggerFactory.getLogger(...)` 手动声明改为 `@Slf4j` 注解。

---

### 3.3 WebAppUtils 含测试代码

**涉及文件**：`property-module-ai/util/WebAppUtils.java`

**修复方案**：删除 `sum()` 和 `removeFile()` 方法。

---

### 3.4 SysConfigService 自注入反模式

**涉及文件**：`property-framework/service/SysConfigService.java`

**修复方案**：将 `@Cacheable` 方法提取到独立的内部 `@Component` 类中，消除自注入。

---

### 3.5 AdminFeeStandardController 分页参数不一致

**涉及文件**：`AdminFeeStandardController.java`

**修复方案**：改为使用 `PageQuery` 对象接收分页参数，与其他 Controller 保持一致。

---

## 执行顺序

```
阶段一（P0，9 项）
  ├── 1.6 删除 BillStatusEnum 死代码（独立，无依赖）
  ├── 1.8 Cookie 常量统一（独立，无依赖）
  ├── 1.7 ROLE 常量统一（独立，无依赖）
  ├── 1.3 Service 去 HttpServletResponse（依赖 1.8）
  ├── 1.2 Controller 去业务逻辑（依赖 1.3）
  ├── 1.1 Controller 去 Mapper（依赖 1.4）
  ├── 1.4 Entity 迁移出 API 模块（依赖 1.1、1.5）
  ├── 1.5 跨模块裸 SQL 替换（依赖 1.4）
  └── 1.9 createBy 硬编码修复（独立，无依赖）

阶段二（P1，8 项，可并行）
  ├── 2.1 generatePaymentNo 统一
  ├── 2.2 PAYMENT_METHOD_NAMES 统一
  ├── 2.3 toLong 统一
  ├── 2.4 PaymentOrderService 重命名
  ├── 2.5 注入方式统一
  ├── 2.6 Token 刷新统一
  ├── 2.7 分页字段统一
  └── 2.8 WebMvcConfig 统一

阶段三（P2，5 项，择机修复）
  ├── 3.1 ImportResult 提取
  ├── 3.2 @Slf4j 统一
  ├── 3.3 WebAppUtils 清理
  ├── 3.4 SysConfigService 重构
  └── 3.5 分页参数统一
```

---

## 影响范围评估

| 阶段   | 涉及模块数 | 预计修改文件数 | 风险                       |
| ------ | ---------- | -------------- | -------------------------- |
| 阶段一 | 6          | ~25            | 中（模块拆分影响依赖关系） |
| 阶段二 | 5          | ~15            | 低                         |
| 阶段三 | 5          | ~8             | 低                         |
