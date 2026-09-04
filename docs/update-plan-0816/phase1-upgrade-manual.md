# 阶段一操作手册：Spring 版本升级 + Redis 引入

> 配套文档：[升级改造计划书](./upgrade-plan.md)
> 编制日期：2026-08-15
> 阶段范围：JDK 21→25、Spring Boot 3.2.0→4.1.0、Redis 引入（缓存/分布式锁）
> 预计影响：全局版本升级 + 框架层 Redis 基础设施 + 支付回调锁替换
> 前置条件：无（本阶段是所有后续阶段的基础）

---

## 目录

- [1. 变更总览](#1-变更总览)
- [2. 环境准备](#2-环境准备)
- [3. Spring 版本升级（步骤 1-7）](#3-spring-版本升级步骤-1-7)
- [4. Redis 引入（步骤 8-17）](#4-redis-引入步骤-8-17)
- [5. 编译与启动验证](#5-编译与启动验证)
- [6. 兼容性问题排查与回退方案](#6-兼容性问题排查与回退方案)
- [7. 验收标准](#7-验收标准)
- [附录 A：完整文件变更清单](#附录-a完整文件变更清单)
- [附录 B：Redis 数据结构与 Key 规划](#附录-bredis-数据结构与-key-规划)

---

## 1. 变更总览

### 1.1 版本变更矩阵

| 组件 | 当前版本 | 目标版本 | 说明 |
|------|---------|---------|------|
| JDK | 21 | **25** (LTS) | Spring Boot 4.x 最低要求 JDK 17，推荐 25 |
| Spring Boot | 3.2.0 | **4.1.0** | 基于 Spring Framework 7 |
| MyBatis-Plus | 3.5.7 (boot3-starter) | **3.5.16** (boot4-starter) | Boot4 需切换 starter artifactId |
| MyBatis-Plus 分页 | 内含于 extension | **独立引入 jsqlparser** | 从 MP 3.5.9 起分页插件拆分到独立模块 |
| Knife4j | 4.5.0 | **4.6.0**（若可用）或切换 SpringDoc | 需确认 Boot4 兼容性，见步骤 6 |
| Lombok | 1.18.30 | **1.18.46+** | JDK 25 兼容性 |
| Redis | 无 | **spring-boot-starter-data-redis** | 由 Spring Boot BOM 管理版本 |
| MySQL | 8.0 | 8.0（不变） | — |
| jjwt | 0.12.5 | 0.12.5（不变） | 已兼容 JDK 25 |

### 1.2 Redis 用途

| 场景 | 当前实现 | 目标实现 |
|------|---------|---------|
| 系统配置缓存 | Spring Cache 本地内存（ConcurrentMapCache） | **RedisCacheManager**（分布式） |
| 分布式锁 | MySQL `GET_LOCK` / `RELEASE_LOCK` | **Redis SET NX EX**（更轻量） |
| Token 黑名单 | 无（JWT 无状态不可吊销） | **Redis Key**（阶段二双 Token 使用） |

> Token 黑名单的 Key 结构在本阶段预留，但实际写入逻辑在阶段二双 Token 改造中实现。本阶段只需在 `RedisUtil` 中提供方法。

### 1.3 变更文件清单

| 操作 | 文件路径 |
|------|---------|
| 修改 | [pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/pom.xml) |
| 修改 | [property-framework/pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/pom.xml) |
| 修改 | [docker/Dockerfile](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/Dockerfile) |
| 修改 | [docker/docker-compose.yaml](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/docker-compose.yaml) |
| 修改 | [docker/.env](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/.env) |
| 修改 | [property-admin-api/src/main/resources/application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/resources/application.yml) |
| 修改 | [property-owner-api/src/main/resources/application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/resources/application.yml) |
| 修改 | [property-task/src/main/resources/application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-task/src/main/resources/application.yml) |
| 修改 | [property-framework/.../config/MyBatisPlusConfig.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/config/MyBatisPlusConfig.java) |
| 新增 | `property-framework/.../config/RedisConfig.java` |
| 新增 | `property-framework/.../util/RedisUtil.java` |
| 修改 | [property-module-bill/.../repository/BillPaymentMapper.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-bill/src/main/java/com/property/module/bill/repository/BillPaymentMapper.java)（删除 GET_LOCK 方法） |
| 修改 | [property-module-payment/.../service/PaymentCallbackService.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-payment/src/main/java/com/property/module/payment/service/PaymentCallbackService.java) |

---

## 2. 环境准备

### 2.1 安装 JDK 25

**Windows（推荐 Eclipse Temurin）：**

1. 访问 [Adoptium JDK 25](https://adoptium.net/zh-CN/temurin/releases/?version=25) 下载 Windows x64 MSI 安装包
2. 安装时勾选 **"Set JAVA_HOME variable"** 和 **"Add to PATH"**
3. 验证安装：

```powershell
java -version
# 预期输出：openjdk version "25.0.x" 2026-xx-xx
```

4. 在 IntelliJ IDEA 中配置：
   - `File → Project Structure → Project SDK → Add SDK → JDK`
   - 选择 JDK 25 安装目录
   - `Project language level` 设为 **25**

**IDE 兼容版本要求：**
- IntelliJ IDEA **2025.1+**（原生支持 JDK 25 和 Spring Boot 4）
- 若使用旧版 IDEA，至少需要 2024.3 + 安装 JDK 25 插件

### 2.2 本地启动 Redis

**方式一：Docker（推荐）**

```powershell
docker run -d --name redis `
  -p 6379:6379 `
  -e TZ=Asia/Shanghai `
  --restart unless-stopped `
  redis:8.0-alpine `
  redis-server --requirepass property_redis_2026
```

**方式二：Windows 原生安装（不推荐生产环境）**

- 下载 [Memurai](https://www.memurai.com/)（Redis 协议兼容的 Windows 原生实现）或使用 WSL2 运行 Redis

**验证连接：**

```powershell
docker exec -it redis redis-cli -a property_redis_2026 ping
# 预期输出：PONG
```

### 2.3 Docker 环境确认

```powershell
docker --version
docker compose version
```

确保 Docker Desktop 正在运行，且已分配至少 4GB 内存。

---

## 3. Spring 版本升级（步骤 1-7）

### 步骤 1：修改根 pom.xml 版本属性

**文件**：[pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/pom.xml)

将 `<properties>` 中的版本号修改如下：

```xml
<properties>
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.target>25</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <spring-boot.version>4.1.0</spring-boot.version>
    <mybatis-plus.version>3.5.16</mybatis-plus.version>
    <jjwt.version>0.12.5</jjwt.version>
    <knife4j.version>4.5.0</knife4j.version>
    <mapstruct.version>1.6.2</mapstruct.version>
    <lombok.version>1.18.46</lombok.version>
    <easyexcel.version>4.0.1</easyexcel.version>
    <alipay-sdk.version>4.39.246.ALL</alipay-sdk.version>
</properties>
```

**改动说明**：
- `maven.compiler.source/target`：21 → 25
- `spring-boot.version`：3.2.0 → 4.1.0
- `mybatis-plus.version`：3.5.7 → 3.5.16
- `lombok.version`：1.18.30 → 1.18.46（JDK 25 兼容）

### 步骤 2：根 pom.xml 切换 MyBatis-Plus Starter

**文件**：[pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/pom.xml)

在 `<dependencyManagement>` 中，将原来的 `mybatis-plus-spring-boot3-starter` 替换为 **boot4 专用 starter**，并新增分页插件独立依赖：

```xml
<!-- MyBatis-Plus (Spring Boot 4 专属) -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
<!-- 从 MP 3.5.9 起，分页插件拆分为独立模块，必须显式引入 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
```

> **关键变化**：Spring Boot 4 对应 `mybatis-plus-spring-boot4-starter`，不能继续使用 `boot3-starter`，否则会出现自动配置类加载失败。

### 步骤 3：根 pom.xml 新增 Redis 依赖管理

**文件**：[pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/pom.xml)

在 `<dependencyManagement>` 的 `<dependencies>` 中添加（位置任意，建议放在 MyBatis-Plus 后面）：

```xml
<!-- Redis (版本由 spring-boot-dependencies BOM 统一管理，无需指定 version) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 步骤 4：property-framework/pom.xml 更新

**文件**：[property-framework/pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/pom.xml)

**4.1** 将 `<properties>` 中的编译版本改为 25：

```xml
<properties>
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.target>25</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

**4.2** 将 MyBatis-Plus 依赖的 artifactId 从 `boot3-starter` 改为 `boot4-starter`，并在其后添加 jsqlparser 依赖和 Redis 依赖：

```xml
<!-- MyBatis-Plus (Spring Boot 4) -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

> `spring-boot-starter-data-redis` 放在 framework 模块，因为所有业务模块都依赖 framework，可传递使用 Redis。

### 步骤 5：检查所有模块 pom.xml 中的编译版本

项目中部分模块可能硬编码了 `<maven.compiler.source>21</maven.compiler.source>`。需要全部改为 25。

执行以下检查（在 IDEA 中全局搜索）：

```
搜索内容：<maven.compiler.source>21
替换为：<maven.compiler.source>25
```

需要检查的模块列表：

| 模块 | 是否需修改 |
|------|-----------|
| property-common | 检查并修改 |
| property-framework | 已在步骤 4 修改 |
| property-module-bill | 检查并修改 |
| property-module-owner | 检查并修改 |
| property-module-payment | 检查并修改 |
| property-module-parking | 检查并修改 |
| property-module-notification | 检查并修改 |
| property-module-statistic | 检查并修改 |
| property-admin-api | 检查并修改 |
| property-owner-api | 检查并修改 |
| property-task | 检查并修改 |

> 如果子模块没有显式声明 compiler.source/target，则会继承根 pom 的 25，无需修改。

### 步骤 6：Knife4j 兼容性处理

**背景**：Knife4j 4.5.0 是为 Spring Boot 3.x 设计的。Spring Boot 4.1.0 基于 Spring Framework 7，可能存在兼容性问题。

**处理策略**：先尝试编译，根据结果选择方案。

**方案 A（首选）**：保持 knife4j 4.5.0，若编译通过则无需改动。

**方案 B（若编译报错）**：升级 knife4j 到支持 Spring Boot 4 的版本（发布后），修改根 pom.xml：

```xml
<knife4j.version>4.6.0</knife4j.version>
```

**方案 C（兜底）**：若 Knife4j 短期不兼容 Boot4，临时移除 knife4j 依赖，仅保留 `springdoc-openapi-starter-webmvc-ui`（Spring Boot 4 的 BOM 已管理其版本）：

```xml
<!-- property-framework/pom.xml 中替换 knife4j 为 springdoc -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

同时修改 [SwaggerConfig.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/config/SwaggerConfig.java) 中 knife4j 相关的注解导入（`@Tag`、`@Operation` 等 springdoc 注解不变，仅移除 knife4j 特有的增强配置）。

application.yml 中的 knife4j 配置段在方案 C 下需删除或注释。

> **注意**：Knife4j 本质上是 springdoc 的 UI 增强，核心注解（`@Tag`、`@Operation`、`@Schema`）来自 springdoc，即使移除 knife4j 也不影响接口文档注解。Swagger UI 仍可通过 `/swagger-ui/index.html` 访问。

### 步骤 7：升级 Dockerfile 基础镜像

**文件**：[docker/Dockerfile](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/Dockerfile)

将两处 `21` 改为 `25`：

```dockerfile
# 第 1 行：构建阶段
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

# ...（中间内容不变）

# 第 41 行：运行阶段
FROM eclipse-temurin:25-jre-alpine
```

> **镜像可用性**：Eclipse Temurin 25 镜像已在 Docker Hub 发布。如果 `maven:3.9-eclipse-temurin-25-alpine` 标签不存在，可使用 `maven:3.9-eclipse-temurin-25`（非 alpine 版本，镜像更大但兼容性更好）。

---

## 4. Redis 引入（步骤 8-17）

### 步骤 8：docker-compose 新增 Redis 服务

**文件**：[docker/docker-compose.yaml](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/docker-compose.yaml)

**8.1** 在 `mysql` 服务定义之后、后端服务之前，添加 Redis 服务：

```yaml
  # ==================== Redis 缓存 ====================
  redis:
    image: redis:8.0-alpine
    container_name: pms-redis
    ports:
      - "6379:6379"
    environment:
      TZ: Asia/Shanghai
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD}
      --maxmemory 256mb
      --maxmemory-policy allkeys-lru
      --appendonly yes
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
```

**8.2** 在文件末尾的 `volumes:` 中新增 Redis 数据卷：

```yaml
volumes:
  mysql-data:
  redis-data:    # 新增
```

**8.3** 为 `admin-api`、`owner-api`、`task` 三个服务添加 Redis 环境变量和依赖。

以 `admin-api` 为例，在 `environment:` 段中添加：

```yaml
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
```

在 `depends_on:` 中添加 Redis 依赖：

```yaml
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
```

对 `owner-api` 和 `task` 做完全相同的修改。

### 步骤 9：.env 文件新增 Redis 密码

**文件**：[docker/.env](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/.env)

在文件中添加（建议放在数据库配置附近）：

```env
# Redis
REDIS_PASSWORD=property_redis_2026
```

> **安全提示**：生产环境请使用更强的随机密码。本地开发可使用上述默认值。

### 步骤 10：application.yml 新增 Redis 配置

三个可部署应用的 `application.yml` 都需要添加 Redis 配置。

**文件列表**：
- [property-admin-api/src/main/resources/application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/resources/application.yml)
- [property-owner-api/src/main/resources/application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/resources/application.yml)
- [property-task/src/main/resources/application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-task/src/main/resources/application.yml)

在每个文件的 `spring:` 节点下，与 `datasource:` 平级，添加 `data:` 配置：

```yaml
spring:
  # ... 已有的 application、lifecycle、datasource 配置 ...

  # Redis 配置（新增）
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 5s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
          max-wait: 3s
```

> **注意**：Spring Boot 3.x/4.x 中 Redis 配置前缀从 `spring.redis` 变更为 `spring.data.redis`。本地开发时若不设置 `REDIS_PASSWORD` 环境变量，默认为空（无密码），但前提是本地 Redis 也未设置密码。

### 步骤 11：新增 RedisConfig 配置类

**新建文件**：`property-framework/src/main/java/com/property/framework/config/RedisConfig.java`

```java
package com.property.framework.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 配置
 *
 * 1. RedisTemplate：Key 用 String 序列化，Value 用 JSON 序列化
 * 2. RedisCacheManager：让 @Cacheable 注解使用 Redis 作为缓存存储（替代默认本地内存）
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate
     * Key 使用 StringRedisSerializer
     * Value 使用 GenericJackson2JsonRedisSerializer（带类型信息，支持反序列化为原始类型）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 用 String 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 用 JSON 序列化
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(buildObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis 缓存管理器
     * 让 Spring Cache 抽象（@Cacheable / @CacheEvict）自动使用 Redis
     * 默认缓存过期时间 30 分钟
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(buildObjectMapper())))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    /**
     * 构建 ObjectMapper，支持 Java 8 时间类型和类型信息
     */
    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }
}
```

**设计说明**：
- `RedisTemplate<String, Object>`：通用 Redis 操作模板，供 `RedisUtil` 使用
- `RedisCacheManager`：自动接管 `@Cacheable`，使 [SysConfigService](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/service/SysConfigService.java) 的系统配置缓存从本地内存迁移到 Redis，**无需修改任何业务代码**
- TTL 30 分钟：系统配置变更后最多 30 分钟自动生效，也可通过 `refreshCache(key)` 主动失效
- `disableCachingNullValues()`：不缓存 null 值，防止缓存穿透

### 步骤 12：新增 RedisUtil 工具类

**新建文件**：`property-framework/src/main/java/com/property/framework/util/RedisUtil.java`

```java
package com.property.framework.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 *
 * 封装常用 Redis 操作，包括：
 * - 基础 KV 操作（set/get/delete/expire/hasKey）
 * - 原子计数（incr/decr）
 * - 分布式锁（tryLock/unlock，基于 SET NX EX）
 *
 * 注意：分布式锁使用简单的 SET NX EX 实现，适用于本项目的低并发场景。
 * 如果未来需要高可靠分布式锁（可重入、红锁、自动续期），可引入 Redisson。
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 基础 KV 操作 ====================

    /** 设置值（永不过期） */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /** 设置值并指定过期时间 */
    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /** 设置值（秒级过期，便捷方法） */
    public void set(String key, Object value, long timeoutSeconds) {
        redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    /** 获取值 */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 获取值并指定类型 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    /** 删除单个 Key */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /** 批量删除 Key */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /** 判断 Key 是否存在 */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /** 设置过期时间 */
    public Boolean expire(String key, Duration timeout) {
        return redisTemplate.expire(key, timeout);
    }

    /** 获取剩余过期时间（秒） */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // ==================== 原子计数 ====================

    /** 自增 1 */
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /** 自增指定步长 */
    public Long incrBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /** 自减 1 */
    public Long decr(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    // ==================== 分布式锁 ====================

    /**
     * 尝试获取分布式锁（非阻塞）
     *
     * @param lockKey   锁 Key（如 "lock:payment:callback:PAY202608150001"）
     * @param requestId 持有者标识（建议用 UUID，用于安全释放锁）
     * @param timeout   锁过期时间（防止死锁）
     * @return true=获取成功，false=获取失败（锁已被其他线程持有）
     */
    public boolean tryLock(String lockKey, String requestId, Duration timeout) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, requestId, timeout);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放分布式锁
     *
     * 使用 Lua 脚本保证"判断持有者 + 删除 Key"的原子性，
     * 防止误删其他线程持有的锁。
     *
     * @param lockKey   锁 Key
     * @param requestId 持有者标识（必须与加锁时一致）
     * @return true=释放成功，false=锁已不属于当前持有者
     */
    public boolean unlock(String lockKey, String requestId) {
        String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "  return redis.call('del', KEYS[1]) " +
                "else " +
                "  return 0 " +
                "end";
        Long result = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                java.util.Collections.singletonList(lockKey),
                requestId);
        return result != null && result > 0;
    }
}
```

**设计说明**：
- 分布式锁使用 `SET key value NX EX seconds` 原子命令（Spring Data Redis 的 `setIfAbsent` 方法）
- 释放锁使用 Lua 脚本保证原子性（GET + DEL 不可分割），防止误删
- `requestId`（UUID）作为锁的持有者标识，只有持有者才能释放
- 这是 Redis 分布式锁的标准实现，足以应对本项目场景

### 步骤 13：移除 MyBatisPlusConfig 上的 @EnableCaching（可选但推荐）

**文件**：[property-framework/.../config/MyBatisPlusConfig.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/config/MyBatisPlusConfig.java)

将 `@EnableCaching` 注解从 `MyBatisPlusConfig` 移到 `RedisConfig` 上（更符合职责分离原则），或者保留原位也可正常工作。

**推荐做法**：在 `RedisConfig` 类上添加 `@EnableCaching`，并从 `MyBatisPlusConfig` 上移除：

```java
// RedisConfig.java
@Configuration
@EnableCaching  // 新增：将缓存启用注解移到 Redis 配置类
public class RedisConfig {
    // ...
}
```

```java
// MyBatisPlusConfig.java —— 移除 @EnableCaching
@Configuration
// @EnableCaching  ← 删除这行
@MapperScan({...})
public class MyBatisPlusConfig {
    // ...
}
```

这样做的原因：`@EnableCaching` 放在 Redis 配置类上更直观——有了 Redis 才有意义，缓存管理器也定义在那里。

### 步骤 14：替换支付回调分布式锁

**14.1 修改 PaymentCallbackService**

**文件**：[property-module-payment/.../service/PaymentCallbackService.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-payment/src/main/java/com/property/module/payment/service/PaymentCallbackService.java)

将 MySQL `GET_LOCK` 替换为 Redis 分布式锁：

```java
package com.property.module.payment.service;

import com.property.framework.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(AlipayService.class)
public class PaymentCallbackService {

    private final AlipayService alipayService;
    private final PaymentCallbackTxService paymentCallbackTxService;
    private final RedisUtil redisUtil;

    /** 分布式锁过期时间：30 秒（足够处理回调事务） */
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(30);

    public String processAlipayNotify(Map<String, String> params) {
        // ========== 1. 验签 ==========
        if (!alipayService.verifyNotify(params)) {
            log.warn("支付宝回调验签失败");
            return "failure";
        }

        AlipayService.PaymentNotifyInfo info = alipayService.parseNotifyParams(params);
        String paymentNo = info.getBillNo();
        if (paymentNo == null || paymentNo.isBlank()) {
            log.warn("支付宝回调缺少商户订单号(out_trade_no)");
            return "failure";
        }

        if (!"TRADE_SUCCESS".equals(info.getTradeStatus())
                && !"TRADE_FINISHED".equals(info.getTradeStatus())) {
            log.info("支付宝通知非成功状态，跳过 [tradeStatus={}]", info.getTradeStatus());
            return "success";
        }

        // ========== 2. Redis 分布式锁 ==========
        String lockKey = "lock:payment:callback:" + paymentNo;
        String requestId = UUID.randomUUID().toString();

        boolean locked = redisUtil.tryLock(lockKey, requestId, LOCK_TIMEOUT);
        if (!locked) {
            log.warn("获取分布式锁失败 [lockKey={}]", lockKey);
            return "failure";
        }

        try {
            // ========== 3. 委托事务处理 ==========
            return paymentCallbackTxService.process(info);
        } catch (Exception e) {
            log.error("处理支付宝回调异常 [paymentNo={}]", paymentNo, e);
            return "failure";
        } finally {
            // ========== 4. 释放锁 ==========
            boolean released = redisUtil.unlock(lockKey, requestId);
            if (!released) {
                log.warn("释放分布式锁失败（锁可能已过期或被他人持有）[lockKey={}]", lockKey);
            }
        }
    }
}
```

**改动要点**：
- 移除 `BillPaymentMapper` 依赖（锁不再需要它）
- 注入 `RedisUtil`
- 锁 Key 格式从 `payment:callback:{paymentNo}` 改为 `lock:payment:callback:{paymentNo}`（统一 `lock:` 前缀，见附录 B）
- 使用 UUID 作为 `requestId`，保证只有持有者能释放锁
- 锁过期时间 30 秒（原 MySQL GET_LOCK 超时为 3 秒等待，但持有时间无上限；Redis 设 30 秒自动过期防止死锁，同时足够事务执行）

**14.2 从 BillPaymentMapper 删除 GET_LOCK 方法**

**文件**：[property-module-bill/.../repository/BillPaymentMapper.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-bill/src/main/java/com/property/module/bill/repository/BillPaymentMapper.java)

删除以下两个方法（及其导入）：

```java
// 删除以下内容：

/** MySQL 分布式锁 — 获取锁（超时 3 秒） */
@Select("SELECT GET_LOCK(#{lockKey}, 3)")
int acquireLock(@Param("lockKey") String lockKey);

/** MySQL 分布式锁 — 释放锁 */
@Select("SELECT RELEASE_LOCK(#{lockKey})")
int releaseLock(@Param("lockKey") String lockKey);
```

删除后，如果 `@Param` 和 `@Select` 在该文件中没有其他用途，也清理对应的 import 语句。

### 步骤 15：确认 SysConfigService 缓存自动生效

**无需修改代码**。[SysConfigService](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/service/SysConfigService.java) 中的 `@Cacheable(value = "sysConfig", key = "#key")` 注解会自动使用步骤 11 中配置的 `RedisCacheManager`。

**验证方式**：启动应用后，首次调用任意系统配置读取（如滞纳金比例），Redis 中会出现 Key：

```
sysConfig::late.fee.rate
```

> Key 格式说明：Spring Cache 默认使用 `{cacheName}::{key}` 作为 Redis Key。

### 步骤 16：系统配置缓存主动失效（已有方法确认）

`SysConfigService.refreshCache(String key)` 方法上已有 `@CacheEvict` 注解，在管理后台修改系统配置时调用此方法即可让缓存立即失效。

搜索当前代码中是否有管理端系统配置的 Controller 和 Service：

- 如果已有管理端配置修改接口，确保修改后调用 `sysConfigService.refreshCache(configKey)`
- 如果没有，本阶段无需新增（缓存有 30 分钟 TTL 自动过期）

### 步骤 17：本地开发环境变量配置

本地开发时（非 Docker），需要在 IDEA 的运行配置中设置环境变量，或在系统环境变量中设置：

**IDEA 运行配置（admin-api / owner-api / task 三个应用都要设置）：**

| 环境变量 | 本地开发值 | 说明 |
|---------|-----------|------|
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | `property_redis_2026` | 若本地 Redis 未设密码则留空 |

**快速验证配置是否正确**：启动任一应用，观察日志中是否出现：

```
Lettuce  - Connected to localhost/<unresolved>:6379
```

---

## 5. 编译与启动验证

### 5.1 清理并编译

在项目根目录执行：

```powershell
# 清理旧编译产物
mvn clean compile -DskipTests
```

如果编译报错，根据错误信息参照 [第 6 节](#6-兼容性问题排查与回退方案) 排查。

### 5.2 打包

```powershell
mvn clean package -DskipTests
```

### 5.3 本地启动验证

**启动顺序**：

1. 确保 MySQL 和 Redis 已在本地运行（或在 Docker 中）
2. 启动 `property-admin-api`（端口 8081）
3. 启动 `property-owner-api`（端口 8084）
4. 启动 `property-task`（端口 8083）

**验证 Redis 连接**：在应用启动日志中确认没有 Redis 连接异常。

**验证系统配置缓存**：

```powershell
# 1. 调用一个触发系统配置读取的接口（如仪表盘或缴费相关）
curl http://localhost:8081/api/admin/dashboard/overview -H "Cookie: token=<your_token>"

# 2. 检查 Redis 中是否有缓存 Key
docker exec -it redis redis-cli -a property_redis_2026 keys "sysConfig::*"
# 预期输出：sysConfig::property.name 等
```

**验证分布式锁**：触发一笔支付宝支付回调（可通过支付宝沙箱模拟），检查：
1. 回调正常处理（返回 "success"）
2. Redis 中短暂出现 `lock:payment:callback:{paymentNo}` Key（回调处理完后自动删除）
3. 同一笔订单重复回调不会重复处理（幂等性由 `PaymentCallbackTxService` 的状态机保证）

### 5.4 Docker Compose 启动验证

```powershell
cd docker

# 构建并启动所有服务（首次会比较慢）
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看 Redis 日志
docker compose logs redis

# 查看 admin-api 日志（确认 Redis 连接成功）
docker compose logs admin-api | Select-String -Pattern "redis|Lettuce"
```

---

## 6. 兼容性问题排查与回退方案

### 6.1 常见编译问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `java: invalid target release: 25` | IDEA 未配置 JDK 25 或 Maven 使用的 JDK 不对 | 检查 Project Structure SDK；检查 Maven Runner JRE |
| `ClassNotFoundException: org.springframework.boot...` | Spring Boot 4 包路径变更 | 确认依赖树使用 4.1.0：`mvn dependency:tree \| findstr spring-boot` |
| `MybatisPlusException: Mybatis Plus Processor` | MP boot4-starter 未正确引入 | 确认 artifactId 是 `mybatis-plus-spring-boot4-starter` 不是 boot3 |
| `PaginationInnerInterceptor` 找不到 | MP 3.5.9+ 分页插件拆分 | 确认已引入 `mybatis-plus-jsqlparser` 依赖 |
| `lombok` 注解不生效 | Lombok 版本不兼容 JDK 25 | 升级到 1.18.46+，并在 IDEA 中更新 Lombok 插件 |
| `javax.servlet.*` 找不到 | Spring Boot 4 完全迁移到 jakarta | 项目已使用 jakarta.servlet，如有遗漏需替换 `javax.*` → `jakarta.*` |
| knife4j 启动报错 | Knife4j 4.5.0 不兼容 Boot4 | 按步骤 6 的方案 B/C 处理 |

### 6.2 Spring Boot 4.x API 变更注意事项

Spring Boot 4.0/4.1 基于 Spring Framework 7，主要变更：

1. **配置属性变更**：
   - `spring.redis.*` → `spring.data.redis.*`（已在步骤 10 处理）
   - 部分 Actuator 端点路径可能调整

2. **废弃 API 移除**：
   - `WebMvcConfigurer` 的一些过时方法可能被移除
   - `RestTemplate` 在 Boot 4 中仍可用但推荐使用 `RestClient`（本阶段不强制改）

3. **Jakarta EE 11**：
   - Servlet 6.1、Validation 3.1
   - 确保所有 import 以 `jakarta.` 开头（项目已是 jakarta，问题不大）

### 6.3 回退方案：降级到 Spring Boot 3.5.x

如果 Spring Boot 4.1.0 遇到不可解决的生态兼容问题（如关键依赖长期不支持 Boot4），按以下步骤回退到 **Spring Boot 3.5.x**（3.x 最新稳定线，同时兼容 JDK 25）：

**回退操作**：

1. 根 pom.xml：
   ```xml
   <spring-boot.version>3.5.13</spring-boot.version>
   ```

2. 根 pom.xml 中 MyBatis-Plus starter 改回 boot3 版本：
   ```xml
   <!-- 改回 boot3-starter -->
   <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
   <!-- 删除 mybatis-plus-jsqlparser（3.5.7 不需要它） -->
   ```

3. property-framework/pom.xml 同步改回 boot3-starter，删除 jsqlparser。

4. MyBatis-Plus 版本可回退到 3.5.12（boot3 最新稳定版）：
   ```xml
   <mybatis-plus.version>3.5.12</mybatis-plus.version>
   ```

5. application.yml 中 Redis 配置保持 `spring.data.redis`（Spring Boot 3.2+ 已使用此前缀）。

6. JDK 保持 25（Spring Boot 3.5.x 完全支持 JDK 25）。

7. Dockerfile 保持 JDK 25 不变。

8. RedisConfig、RedisUtil、分布式锁替换等代码全部保留，不受影响。

> **回退后的技术栈**：JDK 25 + Spring Boot 3.5.13 + Redis。技术含金量与 JDK 25 + Boot 4.1 差距不大（JDK 25 是主要升级点），但生态稳定性更高。

---

## 7. 验收标准

完成本阶段后，逐项确认：

### 7.1 编译与运行

- [ ] `mvn clean package -DskipTests` 编译打包成功，无错误
- [ ] admin-api、owner-api、task 三个应用在 JDK 25 下正常启动
- [ ] IDEA 中 Project SDK 显示为 25
- [ ] `java -version` 输出 25.x

### 7.2 Redis 连接

- [ ] Docker Compose 中 Redis 容器健康状态为 healthy
- [ ] 三个后端应用日志中显示 Redis 连接成功（Lettuce connected）
- [ ] 应用启动后执行 `docker exec redis redis-cli ping` 返回 PONG

### 7.3 系统配置缓存

- [ ] 调用触发配置读取的接口后，Redis 中出现 `sysConfig::*` 格式的 Key
- [ ] 相同配置第二次读取不再查数据库（可通过 MyBatis 日志确认无 SQL 输出）
- [ ] 调用 `refreshCache(key)` 后，对应 Key 从 Redis 中消失

### 7.4 分布式锁

- [ ] 支付回调正常处理，返回 "success"
- [ ] 回调处理期间 Redis 中出现 `lock:payment:callback:*` Key
- [ ] 回调处理完成后锁 Key 自动删除
- [ ] 同一笔订单并发回调只有一个能获取到锁

### 7.5 旧锁清理

- [ ] `BillPaymentMapper` 中不再有 `acquireLock` / `releaseLock` 方法
- [ ] 全局搜索 `GET_LOCK` 和 `RELEASE_LOCK` 无结果
- [ ] `PaymentCallbackService` 中不再注入 `BillPaymentMapper`

### 7.6 Docker 部署

- [ ] `docker compose up -d --build` 全部服务构建成功
- [ ] `docker compose ps` 所有服务状态为 running / healthy
- [ ] 前端页面可正常访问（admin-web 端口 80，owner-web 端口 81）
- [ ] 登录、查账单、缴费等核心功能正常

### 7.7 回归测试

- [ ] 管理端登录正常
- [ ] 业主端登录（含图形验证码）正常
- [ ] 仪表盘数据正常显示
- [ ] 账单查询正常
- [ ] 支付宝支付流程正常（下单 → 支付 → 回调）
- [ ] Excel 导入功能正常
- [ ] XXL-Job 定时任务正常触发

---

## 附录 A：完整文件变更清单

### 修改文件（10 个）

| # | 文件 | 改动摘要 |
|---|------|---------|
| 1 | `pom.xml` | JDK→25、Boot→4.1.0、MP→3.5.16、Lombok→1.18.46；boot3-starter→boot4-starter+jsqlparser；新增 redis 依赖管理 |
| 2 | `property-framework/pom.xml` | 编译版本→25；MP starter→boot4；新增 jsqlparser、data-redis 依赖 |
| 3 | 其余 10 个子模块 `pom.xml` | 编译版本→25（若硬编码了 21） |
| 4 | `docker/Dockerfile` | 基础镜像 temurin-21→temurin-25 |
| 5 | `docker/docker-compose.yaml` | 新增 redis 服务；3 个后端服务添加 REDIS 环境变量和 depends_on |
| 6 | `docker/.env` | 新增 REDIS_PASSWORD |
| 7 | `property-admin-api/src/main/resources/application.yml` | 新增 spring.data.redis 配置 |
| 8 | `property-owner-api/src/main/resources/application.yml` | 新增 spring.data.redis 配置 |
| 9 | `property-task/src/main/resources/application.yml` | 新增 spring.data.redis 配置 |
| 10 | `property-framework/.../config/MyBatisPlusConfig.java` | @EnableCaching 移至 RedisConfig |
| 11 | `property-module-bill/.../BillPaymentMapper.java` | 删除 acquireLock/releaseLock 方法 |
| 12 | `property-module-payment/.../PaymentCallbackService.java` | MySQL GET_LOCK→Redis 分布式锁 |

### 新增文件（2 个）

| # | 文件 | 用途 |
|---|------|------|
| 1 | `property-framework/.../config/RedisConfig.java` | RedisTemplate 序列化 + RedisCacheManager |
| 2 | `property-framework/.../util/RedisUtil.java` | Redis 操作封装 + 分布式锁 |

### 删除文件

无。

---

## 附录 B：Redis 数据结构与 Key 规划

本阶段使用的 Key（阶段二会在此基础上扩展）：

| Key 格式 | 类型 | TTL | 用途 | 本阶段是否使用 |
|---------|------|-----|------|--------------|
| `sysConfig::{configKey}` | String(JSON) | 30 分钟 | Spring Cache 管理的系统配置缓存 | ✅ 是 |
| `lock:payment:callback:{paymentNo}` | String | 30 秒 | 支付回调分布式锁 | ✅ 是 |
| `token:blacklist:{token}` | String | 与 Token 剩余有效期一致 | Access Token 黑名单（阶段二使用） | 🔜 预留 |
| `token:refresh:{userId}` | String | 7 天 | Refresh Token 存储（阶段二使用） | 🔜 预留 |

**Key 命名规范**：

- 统一使用小写字母 + 冒号分层（`业务域:实体:标识`）
- 分布式锁统一以 `lock:` 前缀
- 缓存类 Key 由 Spring Cache 管理（`{cacheName}::{key}` 格式）
- Token 类 Key 以 `token:` 前缀

**Redis 数据库分配**（当前仅使用 db0）：

| Database | 用途 |
|----------|------|
| db0 | 默认库（缓存 + 锁 + Token 全部共用） |

> 本项目规模不需要拆分多个 database，db0 统一管理即可。
