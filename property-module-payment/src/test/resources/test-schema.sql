CREATE TABLE t_bill (
                        id BIGINT PRIMARY KEY,
                        bill_no VARCHAR(32) NOT NULL,
                        room_id BIGINT NOT NULL,
                        owner_id BIGINT NOT NULL,
                        fee_item_id BIGINT NOT NULL,
                        bill_amount DECIMAL(10,2) NOT NULL,
                        paid_amount DECIMAL(10,2) DEFAULT 0,
                        status INT DEFAULT 0,
                        bill_date DATE NOT NULL,
                        due_date DATE NOT NULL,
                        del_flag INT DEFAULT 0,
                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                        update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_payment (
                           id BIGINT PRIMARY KEY,
                           payment_no VARCHAR(32) NOT NULL UNIQUE,
                           bill_id BIGINT NOT NULL,
                           trade_no VARCHAR(64),
                           pay_amount DECIMAL(10,2) NOT NULL,
                           method INT NOT NULL,
                           status INT DEFAULT 0,
                           payer_name VARCHAR(64),
                           notify_time DATETIME,
                           del_flag INT DEFAULT 0,
                           create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                           update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_fee_item (
                            id BIGINT PRIMARY KEY,
                            name VARCHAR(64) NOT NULL,
                            default_amount DECIMAL(10,2),
                            unit VARCHAR(16),
                            del_flag INT DEFAULT 0
);

CREATE TABLE t_sys_config (
                              id BIGINT PRIMARY KEY,
                              config_key VARCHAR(64) NOT NULL,
                              config_value TEXT,
                              del_flag INT DEFAULT 0
);