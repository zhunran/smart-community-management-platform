# 便民服务模块（property-module-lifeservice）设计

> 职责：报修工单、场地预约、访客通行码。
> 依赖：property-framework；房屋信息通过 `RoomDataService`（housing）查询；通知复用 notification 模块的邮件能力（模块间通过 admin-api/owner-api 聚合层编排，lifeservice 模块本身不直接依赖 notification）。
> 命名：使用 `property-module-lifeservice`（包 `com.property.module.lifeservice`），避免与 Spring 的 service 层概念混淆。

---

## 一、模块骨架

```
property-module-lifeservice/
└─ src/main/java/com/property/module/lifeservice/
    ├─ entity/
    │   ├─ RepairOrderEntity.java
    │   ├─ VenueEntity.java
    │   ├─ VenueBookingEntity.java
    │   └─ VisitorPassEntity.java
    ├─ repository/
    │   ├─ RepairOrderMapper.java
    │   ├─ VenueMapper.java
    │   ├─ VenueBookingMapper.java
    │   └─ VisitorPassMapper.java
    ├─ service/
    │   ├─ RepairOrderService.java
    │   ├─ VenueBookingService.java
    │   ├─ VisitorPassService.java
    │   └─ impl/...
    ├─ converter/RepairConverter.java 等
    └─ dto/repair|venue|visitor/...
```

注册步骤同 community 模块（父 pom modules、入口依赖、MapperScan、compiler-plugin 显式声明）。

---

## 二、报修工单

### 2.1 状态机（8 态）

```
业主提交
  → 待审核(0) ─物业通过→ 待派单(1) ─指派维修员→ 已派单(2) ─维修员接单→ 维修中(3)
                 │                                             │
                 │                                        完成并填写处理说明
                 │                                             │
  驳回(6) ←物业驳回┘                                       已完成(4) ─业主评价→ 已评价(5)

  已取消(7) ←─ 业主主动取消（仅 0/1 态可取消）
  超时升级：0/1 态超 24h、2 态超 48h 未接单 → timeout_flag=1 + 通知管理员
```

状态跳转合法性集中在枚举（参照 `BillStatusEnum.canPay()` 模式）：

```java
public enum RepairStatusEnum {
    PENDING_AUDIT(0, "待审核"),
    PENDING_ASSIGN(1, "待派单"),
    ASSIGNED(2, "已派单"),
    REPAIRING(3, "维修中"),
    COMPLETED(4, "已完成"),
    RATED(5, "已评价"),
    REJECTED(6, "已驳回"),
    CANCELED(7, "已取消");

    /** 业主可取消的状态 */
    public boolean canCancel() { return this == PENDING_AUDIT || this == PENDING_ASSIGN; }
    /** 可评价的状态 */
    public boolean canRate() { return this == COMPLETED; }
    /** 维修员可接单/完工的状态 */
    public boolean canHandle() { return this == ASSIGNED; }
    public boolean canFinish() { return this == REPAIRING; }
}
```

### 2.2 工单号生成

复用账单号模式：`RP + yyyyMMdd + 4 位流水`，查当日最大号自增。并发量低（工单非秒杀），查最大号 + 插入即可，无需分布式号段。

### 2.3 超时自动升级（XXL-Job）

```
property-task 新增 RepairTimeoutJob（每小时执行）
  ├─ 扫描 status=0/1 AND create_time < NOW()-24h
  ├─ 扫描 status=2 AND update_time < NOW()-48h（未接单）
  └─ 置 timeout_flag=1 + 发管理员通知（邮件）
```

调度配置加入 `application.yml` 的 xxl-job executor 与调度中心，频率每小时，cron：`0 0 * * * ?`。

### 2.4 接口清单

**管理端（/api/admin/service/repair）**

| 方法 | 路径           | 说明                                 |
| ---- | -------------- | ------------------------------------ |
| GET  | /page          | 工单列表（状态/分类/超时筛选）       |
| GET  | /{id}          | 详情                                 |
| POST | /{id}/audit    | 审核（通过→待派单 / 驳回+原因）      |
| POST | /{id}/assign   | 指派维修员（handlerId）              |
| POST | /{id}/accept   | 维修员接单（维修员登录管理端操作）   |
| POST | /{id}/complete | 完工（处理说明）                     |
| GET  | /statistics    | 工单统计（各状态数量、平均处理时长） |

**业主端（/api/owner/service/repair）**

| 方法 | 路径         | 说明                        |
| ---- | ------------ | --------------------------- |
| POST | /            | 提交报修（文字+图片）       |
| GET  | /mine        | 我的工单                    |
| GET  | /{id}        | 详情 + 处理进度             |
| POST | /{id}/cancel | 取消（0/1 态）              |
| POST | /{id}/rate   | 评价（1-5星+评语，仅 4 态） |

### 2.5 核心实现要点

```java
@Transactional(rollbackFor = Exception.class)
public void audit(Long id, AuditRequest req) {
    RepairOrderEntity order = repairMapper.selectByIdForUpdate(id);  // SELECT ... FOR UPDATE 行级锁
    RepairStatusEnum current = RepairStatusEnum.fromValue(order.getStatus());
    if (current == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "状态非法");
    if (current != RepairStatusEnum.PENDING_AUDIT) throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不可审核");

    if (req.getApproved()) {
        order.setStatus(RepairStatusEnum.PENDING_ASSIGN.getValue());
    } else {
        order.setStatus(RepairStatusEnum.REJECTED.getValue());
        order.setRejectReason(req.getReason());
    }
    repairMapper.updateById(order);
}
```

> 审核是管理端低频操作，但存在"两个管理员同时处理同一单"可能，用 `SELECT ... FOR UPDATE`（金额/状态敏感场景同账单策略）保证串行；接单/完工同理。

**Mapper 层需补充的自定义方法**：

```java
// RepairOrderMapper.java
@Select("SELECT * FROM t_repair_order WHERE id = #{id} FOR UPDATE")
RepairOrderEntity selectByIdForUpdate(@Param("id") Long id);
```

---

## 三、场地预约（第二期）

### 3.1 冲突检测（核心）

```java
@Transactional(rollbackFor = Exception.class)
public void book(Long venueId, Long ownerId, BookingRequest req) {
    // ① 校验场地启用、时段在开放时间内、粒度对齐
    VenueEntity venue = venueMapper.selectById(venueId);
    Assert.isTrue(venue.getStatus() == 1, "场地已停用");
    checkOpenWindow(venue, req);      // start >= open_time, end <= close_time
    checkSlotAligned(venue, req);     // 分钟数 % slotMinutes == 0

    // ② 月度上限校验
    if (venue.getMonthlyLimit() > 0) {
        Long count = bookingMapper.selectCount(...当月已预约数...);
        Assert.isTrue(count < venue.getMonthlyLimit(), "本月预约次数已达上限");
    }

    // ③ 冲突检测：与已有 有效预约 时间区间重叠判断
    Long conflict = bookingMapper.selectCount(
        new LambdaQueryWrapper<VenueBookingEntity>()
            .eq(VenueBookingEntity::getVenueId, venueId)
            .eq(VenueBookingEntity::getBookingDate, req.getDate())
            .in(VenueBookingEntity::getStatus, 0, 1)
            // NOT (existing.end <= new.start OR existing.start >= new.end)
            .apply("NOT (end_time <= {0} OR start_time >= {1})", req.getStartTime(), req.getEndTime()));
    Assert.isTrue(conflict == 0, "该时段已被预约");

    // ④ 插入（并发双抢由下方数据库层兜底）
    bookingMapper.insert(buildEntity(venueId, ownerId, req));
}
```

**并发兜底**：上述"先查后插"在极端并发下可能双抢，由数据库层 `uk_slot` 唯一索引兜底（见 01-database.md 3.2 节 DDL）。冲突时捕获 `DuplicateKeyException`，返回"该时段已被预约"。

**辅助方法定义**：

```java
private void checkOpenWindow(VenueEntity venue, BookingRequest req) {
    if (req.getStartTime().isBefore(venue.getOpenTime().toLocalTime())
            || req.getEndTime().isAfter(venue.getCloseTime().toLocalTime())) {
        throw new BusinessException(ErrorCode.PARAM_ERROR, "预约时段不在场地开放时间内");
    }
}

private void checkSlotAligned(VenueEntity venue, BookingRequest req) {
    long startMin = req.getStartTime().toSecondOfDay() / 60;
    long endMin = req.getEndTime().toSecondOfDay() / 60;
    if (startMin % venue.getSlotMinutes() != 0 || endMin % venue.getSlotMinutes() != 0) {
        throw new BusinessException(ErrorCode.PARAM_ERROR,
                "预约时段需对齐" + venue.getSlotMinutes() + "分钟粒度");
    }
}
```

### 3.2 接口清单

| 端     | 接口                                       | 说明                           |
| ------ | ------------------------------------------ | ------------------------------ |
| 管理端 | CRUD /api/admin/service/venue              | 场地管理                       |
| 管理端 | GET /api/admin/service/venue/{id}/bookings | 预约记录                       |
| 业主端 | GET /api/owner/service/venue/list          | 场地列表                       |
| 业主端 | GET /{id}/slots?date=                      | 当日时段占用图（返回已占区间） |
| 业主端 | POST /{id}/book                            | 预约                           |
| 业主端 | DELETE /booking/{id}                       | 取消（开始前 2h）              |
| 业主端 | GET /booking/mine                          | 我的预约                       |

---

## 四、访客通行码（第二期）

### 4.1 流程

```
业主创建邀请
  → 生成 6 位数字码（SecureRandom + 查库校验唯一，冲突重试 3 次）
  → 返回二维码内容（JSON: {code, validUntil}）供业主转发访客
访客到场 → 门岗扫码/输入码
  → 校验：存在 + status=0 + now ∈ [valid_from, valid_until] + used_count < max_use
  → 通过：used_count+1；达到上限置 status=1
  → 记录通行流水（写入 t_visitor_pass 的同时可扩展 t_access_log，本期省略）
```

### 4.2 通行码生成

```java
// 6位数字，避免 0/1/O/I 混淆的方案更稳，但纯数字便于门岗口头转述 —— 采用纯数字
public String generateCode() {
    // Random 数字 + 查库校验唯一（同一天内唯一即可）
    // 或 SecureRandom 6 位，冲突重试 3 次
}
```

二维码生成：前端用 `qrcode` npm 包将 JSON 串渲染为二维码（后端不生成图片，减少依赖）。

### 4.3 接口清单

| 端     | 接口                                     | 说明                               |
| ------ | ---------------------------------------- | ---------------------------------- |
| 业主端 | POST /api/owner/service/visitor-pass     | 创建（姓名/电话/车牌/有效期/次数） |
| 业主端 | GET /mine                                | 我的邀请列表                       |
| 业主端 | DELETE /{id}                             | 撤销                               |
| 管理端 | GET /api/admin/service/visitor-pass/page | 邀请记录                           |
| 管理端 | POST /verify                             | 门岗核销（code → 通过/拒绝+原因）  |

---

## 五、工作量对照

| 任务                              | 后端   | 备注         |
| --------------------------------- | ------ | ------------ |
| 模块骨架注册                      | 0.5 天 |              |
| 工单提交/列表/详情（业主端）      | 0.5 天 |              |
| 工单审核/派单/接单/完工（管理端） | 0.5 天 | 行级锁       |
| 工单评价 + 统计                   | 0.5 天 |              |
| RepairTimeoutJob（task 模块）     | 0.5 天 | XXL-Job 注册 |
| 场地管理 + 时段占用（二期）       | 0.5 天 |              |
| 场地预约 + 冲突检测（二期）       | 0.5 天 |              |
| 访客通行码（二期）                | 1 天   | 含核销       |
