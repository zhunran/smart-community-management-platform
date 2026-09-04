-- ============================================================
-- 智慧社区改造 - 第一期 DDL
-- 对应: docs/update-plan-0831/01-database.md
-- 表: 活动(2) + 论坛(3) + 报修(1) = 6张
-- ============================================================

-- 2.1 社区活动
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

-- 2.2 活动报名
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

-- 2.3 论坛帖子
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
    sensitive_words VARCHAR(500)        COMMENT '命中的敏感词(逗号分隔,审核参考)',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0待审核 1已发布 2已驳回 3已删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category_status_time (category, status, create_time DESC),
    KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛帖子';

-- 2.4 论坛评论
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

-- 2.5 点赞记录
CREATE TABLE t_forum_like (
    id          BIGINT      NOT NULL,
    target_id   BIGINT      NOT NULL COMMENT '帖子ID或评论ID',
    target_type TINYINT     NOT NULL COMMENT '1帖子 2评论',
    owner_id    BIGINT      NOT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_target_owner (target_type, target_id, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录';

-- 2.6 报修工单
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