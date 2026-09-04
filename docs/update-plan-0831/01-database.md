# 数据库设计（智慧社区改造）

> 共 12 张新表，全部带 `del_flag`（逻辑删除）与 `create_time`/`update_time`，与现有表规范一致。
> 主键策略：`BIGINT` + 雪花 ID（`ASSIGN_ID`），与现有实体一致。

---

## 一、表清单

| 表名                 | 模块      | 说明                      |
| -------------------- | --------- | ------------------------- |
| t_community_activity | community | 社区活动                  |
| t_activity_signup    | community | 活动报名记录              |
| t_forum_post         | community | 论坛帖子                  |
| t_forum_comment      | community | 论坛评论（两级）          |
| t_forum_like         | community | 点赞记录（帖子+评论通用） |
| t_community_vote     | community | 投票主题                  |
| t_vote_option        | community | 投票选项                  |
| t_vote_record        | community | 投票记录                  |
| t_repair_order       | service   | 报修工单                  |
| t_venue              | service   | 场地（健身房/棋牌室等）   |
| t_venue_booking      | service   | 场地预约记录              |
| t_visitor_pass       | service   | 访客通行码                |

> 第二期合计 12 张；第一期先建前 6 张（activity、signup、post、comment、like、repair_order），投票 3 张 + 场地 2 张 + 访客 1 张 = 共 6 张第二期再建。

---

## 二、第一期 DDL

### 2.1 t_community_activity（社区活动）

```sql
CREATE TABLE t_community_activity (
    id               BIGINT       NOT NULL COMMENT '主键(雪花ID)',
    title            VARCHAR(200) NOT NULL COMMENT '活动标题',
    content          TEXT                  COMMENT '活动详情(富文本)',
    cover_image      VARCHAR(500)          COMMENT '封面图URL',
    activity_type    TINYINT      NOT NULL DEFAULT 5 COMMENT '1节日活动 2亲子 3运动 4讲座 5其他',
    location         VARCHAR(200)          COMMENT '活动地点',
    organizer        VARCHAR(100)          COMMENT '主办方',
    start_time       DATETIME     NOT NULL COMMENT '活动开始时间',
    end_time         DATETIME     NOT NULL COMMENT '活动结束时间',
    signup_start     DATETIME              COMMENT '报名开始时间(NULL=不限制)',
    signup_end       DATETIME              COMMENT '报名截止时间',
    max_participants INT          NOT NULL DEFAULT 0 COMMENT '人数上限(0=不限)',
    signup_count     INT          NOT NULL DEFAULT 0 COMMENT '已报名人数(冗余计数)',
    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1招募中 2已满员 3进行中 4已结束 5已取消',
    version          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status_start (status, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区活动';
```

### 2.2 t_activity_signup（活动报名）

```sql
CREATE TABLE t_activity_signup (
    id           BIGINT   NOT NULL,
    activity_id  BIGINT   NOT NULL COMMENT '活动ID',
    owner_id     BIGINT   NOT NULL COMMENT '业主ID',
    participants INT      NOT NULL DEFAULT 1 COMMENT '携带人数(含本人)',
    status       TINYINT  NOT NULL DEFAULT 0 COMMENT '0已报名 1已签到 2已取消',
    signup_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checkin_time DATETIME          COMMENT '签到时间',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_owner (activity_id, owner_id, status),
    KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名记录';
```

> `uk_activity_owner (activity_id, owner_id, status)` 唯一索引：同一活动同一业主的"已报名"记录唯一，已取消(status=2)后可重新报名（新记录 status=0 与已取消记录 status=2 不冲突）。`signup_count` 冗余计数 + 活动表乐观锁控制满员。

### 2.3 t_forum_post（论坛帖子）

```sql
CREATE TABLE t_forum_post (
    id            BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL COMMENT '标题',
    content       TEXT         NOT NULL COMMENT '内容',
    images        JSON                  COMMENT '图片URL数组',
    category      TINYINT      NOT NULL DEFAULT 5 COMMENT '1二手转让 2失物招领 3装修推荐 4邻里互助 5其他',
    owner_id      BIGINT       NOT NULL COMMENT '发帖人',
    room_id       BIGINT                COMMENT '发帖人房屋(用于展示楼栋)',
    view_count    INT          NOT NULL DEFAULT 0,
    like_count    INT          NOT NULL DEFAULT 0 COMMENT '冗余计数',
    comment_count INT          NOT NULL DEFAULT 0 COMMENT '冗余计数',
    is_pinned     TINYINT      NOT NULL DEFAULT 0 COMMENT '置顶',
    is_essence    TINYINT      NOT NULL DEFAULT 0 COMMENT '加精',
    reject_reason VARCHAR(200)          COMMENT '驳回原因',
    sensitive_words VARCHAR(500)          COMMENT '命中的敏感词(逗号分隔,审核参考)',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0待审核 1已发布 2已驳回 3已删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category_status_time (category, status, create_time DESC),
    KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛帖子';
```

> 审核策略：**先发后审** —— 新帖默认 `status=1` 直接可见，敏感词命中则 `status=0` 进待审核，管理端驳回置 `2`。

### 2.4 t_forum_comment（论坛评论）

```sql
CREATE TABLE t_forum_comment (
    id         BIGINT        NOT NULL,
    post_id    BIGINT        NOT NULL COMMENT '帖子ID',
    parent_id  BIGINT        NOT NULL DEFAULT 0 COMMENT '0=一级评论, 非0=回复目标评论ID',
    reply_to   BIGINT        NOT NULL DEFAULT 0 COMMENT '被回复人ownerId(二级展示用)',
    owner_id   BIGINT        NOT NULL COMMENT '评论人',
    content    VARCHAR(1000) NOT NULL,
    like_count INT           NOT NULL DEFAULT 0,
    status     TINYINT       NOT NULL DEFAULT 1 COMMENT '0待审核 1正常 2已删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_post_parent (post_id, parent_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛评论';
```

> **两级评论模型**：`parent_id=0` 为一级评论；二级评论 `parent_id=一级评论id`，用 `reply_to` 标记回复谁。避免无限嵌套的树形组装复杂度（参考主流社区 App 交互）。

### 2.5 t_forum_like（点赞记录）

```sql
CREATE TABLE t_forum_like (
    id          BIGINT      NOT NULL,
    target_id   BIGINT      NOT NULL COMMENT '帖子ID或评论ID',
    target_type TINYINT     NOT NULL COMMENT '1帖子 2评论',
    owner_id    BIGINT      NOT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_target_owner (target_type, target_id, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录';
```

> 点赞高频写，**不设逻辑删除**，取消赞直接物理删除；冗余计数在事务内同步增减。

### 2.6 t_repair_order（报修工单）

```sql
CREATE TABLE t_repair_order (
    id            BIGINT       NOT NULL,
    order_no      VARCHAR(32)  NOT NULL COMMENT '工单号 RP+yyyyMMdd+4位流水',
    owner_id      BIGINT       NOT NULL COMMENT '报修人',
    room_id       BIGINT       NOT NULL COMMENT '报修房屋',
    category      TINYINT      NOT NULL COMMENT '1水电 2门窗 3电梯 4公共设施 5其他',
    title         VARCHAR(200) NOT NULL COMMENT '问题描述(简)',
    description   TEXT         NOT NULL COMMENT '问题详情',
    images        JSON                  COMMENT '现场照片',
    urgency       TINYINT      NOT NULL DEFAULT 1 COMMENT '1普通 2紧急 3特急',
    handler_id    BIGINT                COMMENT '维修员(sys_user)',
    handle_note   VARCHAR(500)          COMMENT '处理说明',
    reject_reason VARCHAR(200)          COMMENT '驳回原因',
    rating        TINYINT               COMMENT '评价1-5星',
    rating_comment VARCHAR(500)         COMMENT '评价内容',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1待派单 2已派单 3维修中 4已完成 5已评价 6已驳回 7已取消',
    timeout_flag  TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1已超时升级',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_owner_status (owner_id, status),
    KEY idx_status_create (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修工单';
```

---

## 三、第二期 DDL

### 3.1 t_community_vote / t_vote_option / t_vote_record

```sql
CREATE TABLE t_community_vote (
    id           BIGINT       NOT NULL,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    vote_type    TINYINT      NOT NULL DEFAULT 1 COMMENT '1单选 2多选',
    is_anonymous TINYINT      NOT NULL DEFAULT 1 COMMENT '1匿名 0实名',
    start_time   DATETIME     NOT NULL,
    end_time     DATETIME     NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区投票';

CREATE TABLE t_vote_option (
    id         BIGINT       NOT NULL,
    vote_id    BIGINT       NOT NULL,
    content    VARCHAR(200) NOT NULL,
    vote_count INT          NOT NULL DEFAULT 0,
    sort_order INT          NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag   TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_vote (vote_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票选项';

CREATE TABLE t_vote_record (
    id          BIGINT   NOT NULL,
    vote_id     BIGINT   NOT NULL,
    option_id   BIGINT   NOT NULL,
    owner_id    BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vote_owner (vote_id, owner_id, option_id),
    KEY idx_option (option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票记录';
```

> `uk_vote_owner (vote_id, owner_id, option_id)` 多选时一人可投多个选项，同一选项不可重复投。

### 3.2 t_venue / t_venue_booking（场地预约）

```sql
CREATE TABLE t_venue (
    id            BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL COMMENT '场地名称',
    venue_type    TINYINT      NOT NULL COMMENT '1健身房 2棋牌室 3会议室 4游泳池 5其他',
    location      VARCHAR(200),
    capacity      INT          NOT NULL DEFAULT 1,
    open_time     TIME         NOT NULL DEFAULT '08:00:00',
    close_time    TIME         NOT NULL DEFAULT '22:00:00',
    slot_minutes  INT          NOT NULL DEFAULT 60 COMMENT '预约粒度(分钟)',
    monthly_limit INT          NOT NULL DEFAULT 0 COMMENT '每业主每月上限(0=不限)',
    price         DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '费用(0=免费)',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地';

CREATE TABLE t_venue_booking (
    id          BIGINT   NOT NULL,
    venue_id    BIGINT   NOT NULL,
    owner_id    BIGINT   NOT NULL,
    booking_date DATE     NOT NULL,
    start_time  TIME     NOT NULL,
    end_time    TIME     NOT NULL,
    status      TINYINT  NOT NULL DEFAULT 0 COMMENT '0已预约 1已使用 2已取消 3已违约',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag    TINYINT   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_slot (venue_id, booking_date, start_time, status),
    KEY idx_venue_date (venue_id, booking_date),
    KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地预约';
```

> 冲突检测：`WHERE venue_id=? AND booking_date=? AND status IN (0,1) AND NOT (end_time <= ? OR start_time >= ?)`，命中即冲突。`uk_slot` 唯一索引为并发兜底：同一场地同时段同日期仅允许一条有效预约。

### 3.3 t_visitor_pass（访客通行码）

```sql
CREATE TABLE t_visitor_pass (
    id           BIGINT       NOT NULL,
    pass_code    VARCHAR(16)  NOT NULL COMMENT '通行码(6-8位数字)',
    owner_id     BIGINT       NOT NULL COMMENT '邀请人',
    visitor_name VARCHAR(50)  NOT NULL,
    visitor_phone VARCHAR(20),
    plate_no     VARCHAR(20)           COMMENT '访客车牌',
    valid_from   DATETIME     NOT NULL,
    valid_until  DATETIME     NOT NULL,
    max_use      INT          NOT NULL DEFAULT 1 COMMENT '可用次数(0=不限)',
    used_count   INT          NOT NULL DEFAULT 0,
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0有效 1已用尽 2已过期 3已撤销',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_owner (owner_id),
    KEY idx_valid (status, valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客通行码';
```

---

## 四、枚举定义（放入 property-common）

| 枚举                   | 值                                                              | 用途       |
| ---------------------- | --------------------------------------------------------------- | ---------- |
| ActivityStatusEnum     | 0草稿/1招募中/2已满员/3进行中/4已结束/5已取消                   | 活动状态机 |
| ActivityTypeEnum       | 1节日/2亲子/3运动/4讲座/5其他                                   | 活动分类   |
| PostCategoryEnum       | 1二手/2失物/3装修/4互助/5其他                                   | 帖子分类   |
| PostStatusEnum         | 0待审核/1已发布/2已驳回/3已删除                                 | 帖子状态   |
| RepairStatusEnum       | 0待审核/1待派单/2已派单/3维修中/4已完成/5已评价/6已驳回/7已取消 | 工单状态机 |
| RepairCategoryEnum     | 1水电/2门窗/3电梯/4公共设施/5其他                               | 工单分类   |
| VoteStatusEnum         | 0未开始/1进行中/2已结束                                         | 投票状态   |
| VenueBookingStatusEnum | 0已预约/1已使用/2已取消/3已违约                                 | 预约状态   |
| VisitorPassStatusEnum  | 0有效/1已用尽/2已过期/3已撤销                                   | 通行码状态 |

枚举实现参照现有 `BillStatusEnum`：含 `value`/`desc`、`fromValue()`、状态跳转判断方法（如 `canSignup()`、`canPay()` 的模式）。
