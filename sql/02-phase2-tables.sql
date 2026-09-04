-- ============================================================
-- 智慧社区改造 - 第二期 DDL
-- 对应: docs/update-plan-0831/01-database.md
-- 表: 投票(3) + 场地(2) + 访客(1) = 6张
-- ============================================================

-- 3.1 社区投票
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

-- 3.1 投票选项
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

-- 3.1 投票记录
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

-- 3.2 场地
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

-- 3.2 场地预约
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

-- 3.3 访客通行码
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