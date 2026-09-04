# 第五阶段落地指南 —— 从"能跑"到"敢交付"

> 对应执行计划：`docs/execution-plan.md` 第五阶段
> 前置条件：Phase 1~4 全部完成
> 预估工期：4~7 天

---

## 目录

- [5.1 Docker 容器化](#51-docker-容器化)
- [5.2 资金链路集成测试](#52-资金链路集成测试)
- [5.3 生产可用性](#53-生产可用性)
- [5.4 云服务器演示部署](#54-云服务器演示部署)
- [5.6 可选速赢项](#56-可选速赢项)

---

## 5.1 Docker 容器化

**目标**：`docker compose up -d` 一键启动全部服务。

**预估**：1.5~2 天

### 前置确认

- [ ] 本地已安装 Docker Desktop（Windows）且 `docker --version` 正常
- [ ] Maven 本地能成功 `mvn clean package -DskipTests`
- [ ] 三个后端模块（admin-api / owner-api / task）打包后 `target/*.jar` 存在
- [ ] `.env.example` 已复制为 `.env` 并填入真实值

### 任务清单

#### T1：创建后端 Dockerfile（多阶段构建）

**文件**：`Dockerfile`（项目根目录）

```dockerfile
# ============================================
# 阶段 1：Maven 编译（复用本地 .m2 缓存）
# ============================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 先复制 pom 文件，利用 Docker 缓存层加速
COPY pom.xml .
COPY property-common/pom.xml property-common/
COPY property-framework/pom.xml property-framework/
COPY property-module-bill/pom.xml property-module-bill/
COPY property-module-owner/pom.xml property-module-owner/
COPY property-module-payment/pom.xml property-module-payment/
COPY property-module-parking/pom.xml property-module-parking/
COPY property-module-notification/pom.xml property-module-notification/
COPY property-module-statistic/pom.xml property-module-statistic/
COPY property-admin-api/pom.xml property-admin-api/
COPY property-owner-api/pom.xml property-owner-api/
COPY property-task/pom.xml property-task/

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -B -q

# 复制源码
COPY property-common/src property-common/src/
COPY property-framework/src property-framework/src/
COPY property-module-bill/src property-module-bill/src/
COPY property-module-owner/src property-module-owner/src/
COPY property-module-payment/src property-module-payment/src/
COPY property-module-parking/src property-module-parking/src/
COPY property-module-notification/src property-module-notification/src/
COPY property-module-statistic/src property-module-statistic/src/
COPY property-admin-api/src property-admin-api/src/
COPY property-owner-api/src property-owner-api/src/
COPY property-task/src property-task/src/

# 编译指定模块 + 依赖模块
ARG MODULE
RUN mvn clean package -pl ${MODULE} -am -DskipTests -q

# ============================================
# 阶段 2：运行镜像（JRE 瘦身）
# ============================================
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

ARG MODULE
COPY --from=builder /build/${MODULE}/target/*.jar app.jar

EXPOSE 8080

# JVM 参数：堆内存上限 512M，OOM 时 dump 堆快照
ENTRYPOINT ["sh", "-c", "java \
  -Xms256m -Xmx512m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/ \
  -jar /app/app.jar"]
```

**验证**：`docker build --build-arg MODULE=property-admin-api -t property-admin-api:latest .`

#### T2：创建前端 Dockerfile

**文件**：`property-admin-web/Dockerfile`

```dockerfile
FROM node:20-alpine AS builder

WORKDIR /build

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
```

**文件**：`property-admin-web/nginx.conf`

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到后端容器
    location /api/ {
        proxy_pass http://admin-api:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**文件**：`property-owner-web/Dockerfile`（内容同上，把 `admin` 替换为 `owner`，`8081` 替换为 `8082`）。

**文件**：`property-owner-web/nginx.conf`（同上，但 `admin-api:8081` → `owner-api:8082`）。

#### T3：创建 docker-compose.yml

**文件**：`docker-compose.yml`（项目根目录）

```yaml
version: '3.8'

services:
  # ==================== 基础设施 ====================
  mysql:
    image: mysql:8.0
    container_name: pms-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_DATABASE: property_management
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./sql/property_management.sql:/docker-entrypoint-initdb.d/01-init.sql:ro
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+08:00
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 40s

  # ==================== 后端服务 ====================
  admin-api:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        MODULE: property-admin-api
    container_name: pms-admin-api
    ports:
      - "8081:8080"
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: root
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      JWT_ADMIN_SECRET: ${JWT_ADMIN_SECRET}
      JWT_EXPIRATION: ${JWT_EXPIRATION}
      SWAGGER_ENABLED: "false"
      MYBATIS_LOG_IMPL: org.apache.ibatis.logging.nologging.NoLoggingImpl
    depends_on:
      mysql:
        condition: service_healthy

  owner-api:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        MODULE: property-owner-api
    container_name: pms-owner-api
    ports:
      - "8082:8080"
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: root
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      JWT_OWNER_SECRET: ${JWT_OWNER_SECRET}
      JWT_EXPIRATION: ${JWT_EXPIRATION}
      ALIPAY_APP_ID: ${ALIPAY_APP_ID}
      ALIPAY_GATEWAY: ${ALIPAY_GATEWAY}
      ALIPAY_MERCHANT_PRIVATE_KEY: ${ALIPAY_MERCHANT_PRIVATE_KEY}
      ALIPAY_ALIPAY_PUBLIC_KEY: ${ALIPAY_ALIPAY_PUBLIC_KEY}
      ALIPAY_NOTIFY_URL: ${ALIPAY_NOTIFY_URL}
      ALIPAY_RETURN_URL: ${ALIPAY_RETURN_URL}
      SWAGGER_ENABLED: "false"
      MYBATIS_LOG_IMPL: org.apache.ibatis.logging.nologging.NoLoggingImpl
    depends_on:
      mysql:
        condition: service_healthy

  task:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        MODULE: property-task
    container_name: pms-task
    ports:
      - "8083:8080"
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: root
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      XXL_JOB_ADMIN_ADDRESSES: ${XXL_JOB_ADMIN_ADDRESSES}
      XXL_JOB_ACCESS_TOKEN: ${XXL_JOB_ACCESS_TOKEN}
      LOG_PATH: ./logs
    depends_on:
      mysql:
        condition: service_healthy

  # ==================== 前端服务 ====================
  admin-web:
    build:
      context: ./property-admin-web
    container_name: pms-admin-web
    ports:
      - "80:80"
    depends_on:
      - admin-api

  owner-web:
    build:
      context: ./property-owner-web
    container_name: pms-owner-web
    ports:
      - "81:80"
    depends_on:
      - owner-api

  # ==================== XXL-Job 调度中心（可选）====================
  xxl-job-admin:
    image: xuxueli/xxl-job-admin:2.4.1
    container_name: pms-xxl-job
    ports:
      - "9099:8080"
    environment:
      PARAMS: >
        --spring.datasource.url=jdbc:mysql://mysql:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
        --spring.datasource.username=root
        --spring.datasource.password=${MYSQL_PASSWORD}
        --xxl.job.accessToken=${XXL_JOB_ACCESS_TOKEN}
    depends_on:
      mysql:
        condition: service_healthy

volumes:
  mysql-data:
```

#### T4：修改数据库连接配置（支持容器内 hostname）

容器内 MySQL 的 hostname 是 `mysql` 而非 `localhost`。需要让 Spring Boot 支持通过环境变量覆盖。

**方法**：在 `application.yml` 中将 `localhost` 替换为 `${MYSQL_HOST:localhost}`。

修改三个文件的同一处：

- `property-admin-api/src/main/resources/application.yml` L8
- `property-owner-api/src/main/resources/application.yml` L8
- `property-task/src/main/resources/application.yml` L23

将：
```yaml
url: jdbc:mysql://localhost:3306/property_management?...
```
改为：
```yaml
url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/property_management?...
```

**验证**：本地启动正常（`MYSQL_HOST` 未设时走默认 `localhost`）。

#### T5：启动验证

```bash
# 1. 启动全部服务
docker compose up -d

# 2. 查看服务状态（全部 healthy）
docker compose ps

# 3. 查看后端日志
docker compose logs admin-api

# 4. 验证 API
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# 5. 验证前端
# 浏览器访问 http://localhost（管理端）
# 浏览器访问 http://localhost:81（业主端）

# 6. 登录验证
# 账号: admin / admin123
```

#### T6：清理命令

```bash
docker compose down           # 停止 + 删除容器
docker compose down -v        # 停止 + 删除容器 + 数据卷（彻底重置）
docker compose logs -f admin-api  # 实时跟踪日志
docker compose restart admin-api  # 单独重启某个服务
```

### 验收标准

- [ ] `docker compose up -d` 全部 healthy，无 restart 循环
- [ ] `http://localhost` 管理端可登录（admin/admin123）
- [ ] `http://localhost:81` 业主端可登录（13900001111/123456）
- [ ] 管理端能生成账单、业主端能查看账单列表
- [ ] Docker 启动不依赖本地 JDK/Maven（给别人用也能跑）

---

## 5.2 资金链路集成测试

**目标**：证明"钱不会出错"。不追求覆盖率数字，只覆盖资金安全关键路径。

**预估**：2~3 天

### 技术选型

| 选型 | 方案 | 理由 |
|------|------|------|
| 测试框架 | JUnit 5 + AssertJ | Spring Boot 3.2 默认 |
| Mock 框架 | Mockito（`@MockBean`） | Spring Boot Test 内置 |
| 数据库 | H2 内存数据库（MySQL 兼容模式） | 不需真实 MySQL，测试秒启 |
| 启动方式 | `@SpringBootTest` + `@AutoConfigureTestDatabase` | 完整 Spring 上下文，Mock 支付宝 Bean |

### T7：test 环境基础设施

**1. 添加 H2 依赖** → `property-module-payment/pom.xml`：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**2. 创建测试配置文件** → `property-module-payment/src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: always
      schema-locations: classpath:test-schema.sql
      data-locations: classpath:test-data.sql

mybatis-plus:
  global-config:
    db-config:
      id-type: ASSIGN_ID

# 关闭支付宝真实连接
alipay:
  app-id: mock-app-id
  gateway: https://mock-gateway
  merchant-private-key: mock-key
  alipay-public-key: mock-key
  notify-url: http://localhost/notify
  return-url: http://localhost/return
```

**3. 创建测试 Schema** → `property-module-payment/src/test/resources/test-schema.sql`：

```sql
-- 只创建支付链路涉及的表（不需要全量 34 张表）
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
```

**4. 创建测试数据** → `property-module-payment/src/test/resources/test-data.sql`：

```sql
-- 一条待支付账单
INSERT INTO t_bill VALUES
(1, 'BILL202608010001', 101, 1001, 1, 1500.00, 0, 0, '2026-08-01', '2026-09-01', 0, NOW(), NOW());

INSERT INTO t_fee_item VALUES
(1, '物业费', 2.5, '元/㎡', 0);
```

**5. 创建测试基类** → `property-module-payment/src/test/java/com/property/module/payment/BasePaymentTest.java`：

```java
package com.property.module.payment;

import com.alipay.api.AlipayClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = PaymentTestApplication.class,  // 轻量启动类（见下方）
    webEnvironment = SpringBootTest.WebEnvironment.NONE  // 不启动 Web 容器
)
@ActiveProfiles("test")
public abstract class BasePaymentTest {

    @MockBean
    protected AlipayClient alipayClient;  // Mock 支付宝 SDK
}
```

> 关于 `PaymentTestApplication`：如果 `@SpringBootTest` 指定 property-admin-api 的启动类会扫描太多模块导致启动慢，可以创建一个测试专用的轻量启动类（只扫描 payment + bill + framework 模块），但实际执行时如果扫描范围导致 Bean 缺失或冲突，直接简化成：
> ```java
> @SpringBootTest
> @ActiveProfiles("test")
> ```
> 用 `PropertyAdminApplication` 或 `PropertyOwnerApplication` 作为启动类也可以，H2 开销不大，启动 3~5 秒可以接受。

### T8-T11：核心测试场景

**文件**：`property-module-payment/src/test/java/com/property/module/payment/service/PaymentSafetyTest.java`

```java
package com.property.module.payment.service;

import com.property.module.payment.BasePaymentTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSafetyTest extends BasePaymentTest {

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private PaymentCallbackService paymentCallbackService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ================================================================
    // T8（对应执行计划 T61）：同一账单重复请求 → 幂等，不重复生成订单
    // ================================================================
    @Test
    @DisplayName("同一账单重复创建支付订单 → 返回同一订单")
    void shouldGenerateOrderOnlyOnce() {
        // given: 一条待支付账单 (billId=1)
        Long billId = 1L;

        // when: 两次请求创建支付订单
        var order1 = paymentOrderService.createPaymentOrder(billId);
        var order2 = paymentOrderService.createPaymentOrder(billId);

        // then: 两次返回同一订单
        assertThat(order1).isNotNull();
        assertThat(order2.getPaymentNo()).isEqualTo(order1.getPaymentNo());

        // then: 数据库中只有一条支付记录
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_payment WHERE bill_id = ?", Integer.class, billId);
        assertThat(count).isEqualTo(1);
    }

    // ================================================================
    // T9（对应执行计划 T63）：支付宝重复回调 → 只处理一次
    // ================================================================
    @Test
    @DisplayName("支付宝重复回调 → 不重复入账")
    void shouldHandleDuplicateNotify() {
        // given: 已创建支付订单，账单未支付
        // when: 模拟两次相同的支付宝回调通知
        // then: 第一次回调 → 账单状态变已支付，金额入账
        // then: 第二次回调 → 不改变任何数据，日志记录"重复通知"
        // TODO: 具体实现依赖 PaymentCallbackService 的实际接口
    }

    // ================================================================
    // T10（对应执行计划 T62）：并发线下缴费 → 行锁防超收
    // ================================================================
    @Test
    @DisplayName("两个操作员同时标记同一账单已缴费 → 第二个被阻塞")
    void shouldLockBillDuringOfflinePay() throws Exception {
        // given: 一条待支付账单
        // when: 两个线程同时执行线下缴费标记
        // then: 只有一个成功，另一个抛出异常或被拒绝
        // TODO: 使用 ExecutorService 模拟并发
    }

    // ... 更多测试场景见执行计划 T64-T70
}
```

> **注意**：测试方法的具体实现依赖于 `PaymentOrderService` 和 `PaymentCallbackService` 的实际接口签名。在编写测试前，先确认这些 Service 的方法签名是否支持测试所需的参数（某些方法可能内部依赖支付宝调用，需要额外 Mock）。

### 验收标准

- [ ] `mvn test -pl property-module-payment -am` 全部通过
- [ ] 6 个 P0 级测试至少 4 个能跑通
- [ ] 每个测试方法名使用 `shouldXxxWhenYyy` 命名，一眼能看出测试意图
- [ ] 不依赖外部 MySQL / 支付宝 / Redis

---

## 5.3 生产可用性

**目标**：健康检查、优雅停机、结构化日志——半小时搞定的事，面试时是区分"学生项目"和"交付项目"的关键信号。

**预估**：0.5 天

### T12：Actuator 健康检查

**1. 三个 API 模块的 `pom.xml` 各自添加依赖**：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**说明**：可以只在 `property-admin-api`、`property-owner-api`、`property-task` 这三个启动模块中添加（`spring-boot-starter-actuator` 已经在 Spring Boot 依赖管理中声明了版本）。

**2. 三个 API 模块的 `application.yml` 各自添加**：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true  # 启用就绪探针 / 存活探针
```

**验证**：

```bash
# 启动后访问
curl http://localhost:8081/actuator/health
# 预期：{"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"},"ping":{"status":"UP"}}}

curl http://localhost:8081/actuator/health/readiness
# 预期：{"status":"UP"}
```

### T13：优雅停机

三个 API 模块的 `application.yml` 各自添加：

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

**验证**：

```bash
# 启动服务 → 发送一个耗时请求（如长查询）→ Ctrl+C 停止
# 预期：服务不立即退出，30 秒内处理完进行中请求，新请求直接拒绝
```

### T14：Docker 健康检查

`Dockerfile` 中已有的 `RUN apk add --no-cache curl` 就是为了这个。在 `Dockerfile` 末尾 `ENTRYPOINT` 之前添加：

```dockerfile
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

`docker-compose.yml` 中已配置的 `depends_on` + `condition: service_healthy` 会依赖这个健康检查。

### T15：结构化日志（JSON 格式）

> 当前 `TraceFilter` 已将 `traceId` 写入 MDC，但没有 `logback-spring.xml` 文件来在日志中打印它。

**创建文件**：`property-admin-api/src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console appender (dev 环境：人类可读) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{traceId}] - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- JSON appender (prod 环境：机器可解析) -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <customFields>{"app":"property-admin-api"}</customFields>
        </encoder>
    </appender>

    <!-- dev profile: 可读格式 -->
    <springProfile name="default,dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE" />
        </root>
    </springProfile>

    <!-- prod profile: JSON 格式 -->
    <springProfile name="prod">
        <root level="WARN">
            <appender-ref ref="JSON" />
        </root>
    </springProfile>
</configuration>
```

> **注意**：JSON 格式的 `LogstashEncoder` 需要额外依赖：
> ```xml
> <dependency>
>     <groupId>net.logstash.logback</groupId>
>     <artifactId>logstash-logback-encoder</artifactId>
>     <version>7.4</version>
> </dependency>
> ```
> 如果不想加新依赖，可以简化方案——直接用 pattern 拼 JSON：
> ```xml
> <pattern>{"ts":"%d{yyyy-MM-dd HH:mm:ss.SSS}","level":"%level","logger":"%logger{36}","traceId":"%X{traceId}","msg":"%msg"}%n</pattern>
> ```

**对 `property-owner-api` 和 `property-task` 做同样的操作**（复制 `logback-spring.xml`，修改 `customFields` 中的 `app` 值）。

### 验收标准

- [ ] `GET /actuator/health` 返回 `{"status":"UP"}`
- [ ] Ctrl+C 后服务优雅退出（不立即强制关闭）
- [ ] `docker compose up` 后 `docker compose ps` 显示所有容器 `healthy`
- [ ] 日志中包含 `[traceId]`（dev 环境可读格式）
- [ ] Docker 启动时命令行输出中没有 SQL 日志和 Swagger 信息

---

## 5.4 云服务器演示部署

**目标**：给面试官一个公网可访问的地址。

**预估**：0.5 天（前提：Docker 镜像构建完成）

### 执行步骤

```
1. 购买阿里云/腾讯云轻量服务器 2C2G（~68 元/月，或免费试用 3 个月）
2. 安装 Docker CE + Docker Compose
3. git clone <你的仓库地址>
4. cp .env.example .env  →  编辑填入真实值（尤其支付宝沙箱密钥）
5. docker compose up -d
6. 安全组开放端口：80, 81, 8081, 9099
7. 确认访问 http://<公网IP> 可登录管理端
```

### 演示准备

| 项 | 内容 |
|---|------|
| 演示地址 | `http://<公网IP>`（管理端）/ `http://<公网IP>:81`（业主端） |
| 管理端账号 | admin / admin123 |
| 业主端账号 | 13900001111 / 123456 |
| 演示流程 | 登录管理端 → 生成账单 → 业主端看账单 → 支付宝沙箱缴费 → 管理端看到入账 |

> 只要 Docker 成功了，这一步就是纯操作，不需要写代码。

---

## 5.6 可选速赢项

以下改动性价比极高，每项 <= 30 分钟，有余力就做：

### 速赢 1：API 版本号前缀（15 分钟）

三个 API 模块的 `application.yml` 添加：

```yaml
server:
  servlet:
    context-path: /api/v1
```

> 连代码都不用改——原有 Controller 的 `@RequestMapping` 会自动拼接 `/api/v1` 前缀。
> 
> **面试话术**："所有 API 都加了 `/api/v1/` 前缀，为未来不兼容的 API 变更留出 `/api/v2/` 的空间。"

### 速赢 2：CORS 配置（已存在）

[`CorsConfig.java`](file:///d:\.workspace\javaproject\property-management-system\property-management\property-framework\src\main\java\com\property\framework\config\CorsConfig.java) 已经存在且配置完整。不需要额外操作。

> **面试话术**："CORS 配置白名单化，没有使用 `*` 通配符，`X-Trace-Id` 也显式暴露给前端用于问题排查。"

### 速赢 3：SQL 初始化自动化（已覆盖）

`docker-compose.yml` 中已通过 `./sql/property_management.sql:/docker-entrypoint-initdb.d/01-init.sql` 挂载实现——MySQL 容器首次启动时自动执行建库脚本。

---

## 执行顺序总览

```
Day 1~2：5.1 Docker 容器化（最优先）
  ├─ T1: 后端 Dockerfile（多阶段构建）
  ├─ T2: 前端 Dockerfile + nginx.conf
  ├─ T3: docker-compose.yml
  ├─ T4: 数据源配置改为 ${MYSQL_HOST:localhost}
  ├─ T5: 启动验证
  └─ T6: 清理命令文档

Day 3~5：5.2 资金链路集成测试（可与 Docker 并行开始）
  ├─ T7: H2 测试基础设施 + 测试基类
  ├─ T8: 幂等性测试
  ├─ T9: 重复回调测试
  └─ T10-T11: 并发 + 异常路径测试

穿插碎片时间（在等 Docker 构建或测试运行的空隙）：
  5.3 生产可用性
  ├─ T12: Actuator 健康检查（15 min）
  ├─ T13: 优雅停机（10 min）
  ├─ T14: Docker HEALTHCHECK（15 min）
  ├─ T15: logback-spring.xml + traceId（30 min）
  └─ 可选速赢项（30 min）

Day 5~7：5.4 云部署（前提：Docker 完成）
  └─ 购买服务器 → 安装 Docker → clone → docker compose up → 安全组配置
```

---

## 验收清单

全部完成时，以下 8 项应全部打勾：

- [ ] `docker compose up -d` 一键启动，6 个容器全部 healthy
- [ ] `http://localhost` 管理端可登录、生成账单、查看支付记录
- [ ] `http://localhost:81` 业主端可登录、查看账单
- [ ] `mvn test -pl property-module-payment -am` 绿色（至少 4 个 P0 测试通过）
- [ ] `curl localhost:8081/actuator/health` 返回 `UP`
- [ ] Docker 日志中每行带 `[traceId]`，无 SQL 输出，无 Swagger 信息
- [ ] Ctrl+C 后服务优雅停机（30s 内处理完进行中请求）
- [ ] API 路径全部以 `/api/v1/` 开头
