# 阶段三操作手册：Spring AI + DeepSeek 流式对话

> 配套文档：[升级改造计划书](./upgrade-plan.md)
> 编制日期：2026-08-17
> 阶段范围：Spring AI 框架引入、DeepSeek 大模型集成、AI 物业客服（SSE 流式对话）、聊天历史存储
> 预计影响：新增 `property-module-ai` 模块 + 业主端新增 AI 客服页面
> 前置条件：阶段一（Redis 已就绪，可缓存系统提示词）已完成；阶段二（双 Token 认证）已完成（AI 客服属于业主端鉴权接口）

---

## 目录

- [1. 变更总览](#1-变更总览)
- [2. 环境准备](#2-环境准备)
- [3. Spring AI 框架引入（步骤 1-4）](#3-spring-ai-框架引入步骤-1-4)
- [4. AI 模块开发（步骤 5-10）](#4-ai-模块开发步骤-5-10)
- [5. 前端改造（步骤 11-13）](#5-前端改造步骤-11-13)
- [6. 编译与验证](#6-编译与验证)
- [7. 验收标准](#7-验收标准)
- [附录 A：完整文件变更清单](#附录-a完整文件变更清单)
- [附录 B：聊天历史表结构](#附录-b聊天历史表结构)
- [附录 C：AI 客服对话流程时序](#附录-cai-客服对话流程时序)

---

## 1. 变更总览

### 1.1 技术引入矩阵

| 组件 | 当前版本 | 目标版本 | 说明 |
|------|---------|---------|------|
| Spring AI | 无 | **2.0.0** | Spring 官方 AI 框架，2026.6 GA |
| DeepSeek API | 无 | **deepseek-v4-flash**（推荐）/ **deepseek-v4-pro** | OpenAI 兼容协议，无需额外 SDK |
| SSE | 无 | **Flux\<String\>** | Spring AI 内置流式支持 |
| 聊天历史 | 无 | **MySQL t_chat_history** | 新建表，按用户+会话存储 |

### 1.2 功能变更矩阵

| 功能 | 当前状态 | 目标状态 |
|------|---------|---------|
| AI 物业客服 | 无 | 业主端可流式对话，支持多轮上下文 |
| 系统提示词 | 无 | 从 `sys_config` 读取，后台可配置 |
| 聊天历史 | 无 | 按用户存储，支持翻页查询 |
| 流式响应 | 无 | SSE 打字机效果 |

### 1.3 变更文件清单

| 操作 | 文件路径 |
|------|---------|
| 修改 | [pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/pom.xml)（新增 Spring AI BOM 依赖管理 + Spring Milestones 仓库） |
| 修改 | [property-framework/pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/pom.xml)（新增 Spring AI OpenAI starter） |
| 修改 | [property-framework/.../config/MyBatisPlusConfig.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/config/MyBatisPlusConfig.java)（新增 AI 模块 Mapper 扫描路径） |
| 新增 | [property-module-ai/pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-ai/pom.xml) |
| 新增 | `property-module-ai/src/main/java/.../config/AiConfig.java` |
| 新增 | `property-module-ai/src/main/java/.../service/AiChatService.java` |
| 新增 | `property-module-ai/src/main/java/.../entity/ChatHistory.java` |
| 新增 | `property-module-ai/src/main/java/.../repository/ChatHistoryMapper.java` |
| 新增 | `property-module-ai/src/main/java/.../dto/ChatRequest.java` |
| 新增 | `property-module-ai/src/main/java/.../dto/ChatHistoryVO.java` |
| 修改 | [property-owner-api/pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/pom.xml)（依赖 property-module-ai） |
| 新增 | `property-owner-api/src/main/java/.../controller/ChatController.java` |
| 修改 | [property-owner-api/src/main/resources/application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/resources/application.yml)（新增 spring.ai 配置） |
| 修改 | [docker/.env](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/.env)（新增 DEEPSEEK_API_KEY） |
| 修改 | [docker/docker-compose.yaml](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/docker-compose.yaml)（owner-api 新增 DEEPSEEK 环境变量） |
| 新增 | `property-owner-web/src/api/chat.ts` |
| 新增 | `property-owner-web/src/views/chat/ChatView.vue` |
| 修改 | `property-owner-web/src/router/index.ts`（新增 /chat 路由） |
| 修改 | `property-owner-web/src/views/home/HomeView.vue`（新增 AI 客服入口） |

---

## 2. 环境准备

### 2.1 申请 DeepSeek API Key

1. 访问 [DeepSeek 开放平台](https://platform.deepseek.com/) 注册账号
2. 进入 [API Keys](https://platform.deepseek.com/api_keys) 页面，点击「创建 API Key」
3. 复制生成的 Key（格式为 `sk-xxxx`），妥善保存

> **费用说明**：DeepSeek 采用按量计费，deepseek-v4-flash 模型约 ￥0.22/百万 token（输入），deepseek-v4-pro 约 ￥0.66/百万 token。推荐使用 v4-flash 用于客服场景。新用户有免费额度。

### 2.2 确认 Spring Milestones 仓库可访问

Spring AI 2.0.0 为 GA 版本，发布在 Maven Central。如果 Maven Central 不可用，可从 Spring Milestones 仓库拉取。确认网络可以访问：

```
https://repo.spring.io/milestone
```

如果公司网络受限，需提前配置代理或镜像。

---

## 3. Spring AI 框架引入（步骤 1-4）

### 步骤 1：根 pom.xml 新增 Spring AI BOM 和 Milestones 仓库

**文件**：[pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/pom.xml)

**1.1** 在 `<repositories>` 中新增 Spring Milestones 仓库（放在现有仓库之后）：

```xml
<repositories>
    <!-- 现有仓库不变 -->

    <!-- Spring Milestones（Spring AI 等 Spring 生态组件） -->
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

**1.2** 在 `<properties>` 中新增版本号：

```xml
<spring-ai.version>2.0.0</spring-ai.version>
```

**1.3** 在 `<dependencyManagement>` 中新增 Spring AI BOM（放在现有 BOM 之后）：

```xml
<!-- Spring AI BOM -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>${spring-ai.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### 步骤 2：property-framework/pom.xml 新增 Spring AI OpenAI Starter

**文件**：[property-framework/pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/pom.xml)

在 `<dependencies>` 中新增（位置任意，建议放在 Redis 依赖之后）：

```xml
<!-- Spring AI OpenAI Starter（兼容 DeepSeek 等 OpenAI 协议模型） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

> **说明**：DeepSeek API 完全兼容 OpenAI 协议，只需改 `base-url`，不需要专门的 DeepSeek Starter。Spring AI 2.0 官方文档中已将 DeepSeek 列为 OpenAI-compatible servers 之一。放在 framework 模块，所有业务模块可传递使用。

### 步骤 3：property-owner-api application.yml 新增 Spring AI 配置

**文件**：[property-owner-api/src/main/resources/application.yml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/resources/application.yml)

在 `spring:` 节点下新增（与 `data:` 平级）：

```yaml
spring:
  # ... 已有的 datasource、data.redis 等配置 ...

  # Spring AI 配置（新增）
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY:}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-v4-flash
          temperature: 0.7
      embedding:
        enabled: false
```

**配置说明**：

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `api-key` | `${DEEPSEEK_API_KEY:}` | 从环境变量读取，默认空（未配置时启动报错，见步骤 4） |
| `base-url` | `https://api.deepseek.com` | DeepSeek API 端点（不是 v1 子路径，Spring AI 会自动拼接 `/v1/chat/completions`） |
| `model` | `deepseek-v4-flash`（推荐） | 快速通用对话；复杂推理场景可用 `deepseek-v4-pro` |
| `temperature` | `0.7` | 创造性控制，0~1 越高越发散 |
| `embedding.enabled` | `false` | DeepSeek 暂不支持 Embedding，显式禁用 |

> **为什么用 `spring.ai.openai` 前缀而不是 `spring.ai.deepseek`？**  
> Spring AI 2.0 的 OpenAI Starter 通过 `base-url` 切换服务商。DeepSeek 的 API 协议与 OpenAI 完全兼容，直接复用 OpenAI Starter 是最简方案。Spring AI 社区也推荐这种方式接入 OpenAI-compatible 的第三方模型。

### 步骤 4：docker 环境变量配置

**4.1 .env 文件**

**文件**：[docker/.env](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/.env)

在文件中新增（建议放在 Redis 配置之后）：

```env
# DeepSeek AI
DEEPSEEK_API_KEY=sk-your-deepseek-api-key-here
```

**4.2 docker-compose.yaml**

**文件**：[docker/docker-compose.yaml](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/docker-compose.yaml)

在 `owner-api` 服务的 `environment:` 段中新增：

```yaml
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}
```

---

## 4. AI 模块开发（步骤 5-10）

### 步骤 5：创建 property-module-ai 模块

**5.1 新建模块目录**

在项目根目录下创建 `property-module-ai` 目录，包含以下子目录：

```
property-module-ai/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── property/
        │           └── module/
        │               └── ai/
        │                   ├── config/
        │                   │   └── AiConfig.java
        │                   ├── service/
        │                   │   └── AiChatService.java
        │                   ├── entity/
        │                   │   └── ChatHistory.java
        │                   ├── repository/
        │                   │   └── ChatHistoryMapper.java
        │                   └── dto/
        │                       ├── ChatRequest.java
        │                       └── ChatHistoryVO.java
        └── resources/
```

**5.2 pom.xml**

**新建文件**：`property-module-ai/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.example</groupId>
        <artifactId>property-management</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>property-module-ai</artifactId>
    <name>property-module-ai</name>
    <description>AI 物业客服模块</description>

    <dependencies>
        <!-- 依赖 framework（可传递使用 Spring AI、Redis等） -->
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>property-framework</artifactId>
            <version>${project.parent.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

**5.3 注册到根 pom.xml**

**文件**：[pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/pom.xml)

在 `<modules>` 中新增：

```xml
<module>property-module-ai</module>
```

### 步骤 6：property-owner-api 依赖新模块

**文件**：[property-owner-api/pom.xml](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/pom.xml)

在 `<dependencies>` 中新增：

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>property-module-ai</artifactId>
    <version>${project.parent.version}</version>
</dependency>
```

### 步骤 7：新增 AiConfig 配置类

**新建文件**：`property-module-ai/src/main/java/com/property/module/ai/config/AiConfig.java`

```java
package com.property.module.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置
 *
 * ChatClient 用于同步对话，Flux<String> 用于 SSE 流式对话。
 * ChatMemory 由 Spring AI 自动配置提供（默认 MessageWindowChatMemory + InMemoryChatMemoryRepository）。
 */
@Configuration
public class AiConfig {

    /**
     * ChatClient Bean
     * 注入 ChatClient.Builder（Spring AI 自动配置）和 ChatMemory（自动配置）
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
```

**设计说明**：
- `ChatClient.Builder` 由 Spring AI 自动配置提供（根据 `application.yml` 中的 `spring.ai.openai.*` 配置自动创建 `ChatModel` 和 `ChatClient.Builder`）
- **`ChatMemory` 无需手动定义 Bean**：Spring AI 2.0.0 的 `ChatMemoryAutoConfiguration` 会自动注册 `MessageWindowChatMemory`（内部使用 `InMemoryChatMemoryRepository`），管理当前会话的多轮对话上下文（存储在内存中，服务重启后失效；聊天历史持久化走 MySQL `t_chat_history` 表）
- `MessageChatMemoryAdvisor` 自动将历史消息注入到每次请求中，实现多轮对话
- **Spring AI 2.0.0 API 变更**：`InMemoryChatMemory` 已移除，改为 `MessageWindowChatMemory` + `InMemoryChatMemoryRepository`；`MessageChatMemoryAdvisor` 构造函数已废弃，改为 Builder 模式

### 步骤 8：新增 AiChatService 业务服务

**新建文件**：`property-module-ai/src/main/java/com/property/module/ai/service/AiChatService.java`

```java
package com.property.module.ai.service;

import com.property.framework.service.SysConfigService;
import com.property.module.ai.dto.ChatHistoryVO;
import com.property.module.ai.dto.ChatRequest;
import com.property.module.ai.entity.ChatHistory;
import com.property.module.ai.repository.ChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 客服服务
 *
 * 封装对话逻辑：系统提示词加载、流式对话、聊天历史存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    /** 系统提示词配置 Key */
    private static final String PROMPT_CONFIG_KEY = "ai.chat.system_prompt";
    /** 默认系统提示词 */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是"AI智慧社区服务平台"的智能客服助手，为用户提供物业管理相关咨询服务。
            你的服务范围包括：
            1. 缴费咨询：物业费、水费、电费、燃气费等账单查询与缴费方式
            2. 业务咨询：报修流程、车位租赁、访客登记等
            3. 社区公告：社区活动、停水停电通知等
            请用友好、专业的语气回答，回答简洁明了，每次不超过150字。
            如果你不知道答案，请引导用户联系物业服务中心。""";

    private final ChatClient chatClient;
    private final SysConfigService sysConfigService;
    private final ChatHistoryMapper chatHistoryMapper;

    /**
     * 流式对话（SSE）
     *
     * @param request 对话请求（含 userId、message）
     * @return 流式响应 Flux<String>
     */
    public Flux<String> chatStream(ChatRequest request) {
        String systemPrompt = getSystemPrompt();
        String userMessage = request.getMessage();

        // 保存用户消息到历史
        saveHistory(request.getUserId(), "user", userMessage);

        // 构建 Prompt 并流式调用
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content()
                .doOnError(e -> log.error("AI 对话异常 [userId={}]", request.getUserId(), e));
    }

    /**
     * 保存 AI 回复到聊天历史（由 Controller 在流结束后调用）
     */
    public void saveAssistantMessage(Long userId, String content) {
        saveHistory(userId, "assistant", content);
    }

    /**
     * 查询聊天历史（按用户分组，最新在前）
     */
    public List<ChatHistoryVO> getHistory(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<ChatHistory> list = chatHistoryMapper.selectByUserId(userId, offset, size);
        return list.stream().map(h -> {
            ChatHistoryVO vo = new ChatHistoryVO();
            vo.setId(h.getId());
            vo.setRole(h.getRole());
            vo.setContent(h.getContent());
            vo.setCreateTime(h.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取系统提示词：优先从 sys_config 读取，未配置则用默认值
     */
    private String getSystemPrompt() {
        try {
            String custom = sysConfigService.getString(PROMPT_CONFIG_KEY, null);
            if (custom != null && !custom.isBlank()) {
                return custom;
            }
        } catch (Exception e) {
            log.warn("读取系统提示词配置失败，使用默认提示词", e);
        }
        return DEFAULT_SYSTEM_PROMPT;
    }

    /**
     * 保存聊天历史
     */
    private void saveHistory(Long userId, String role, String content) {
        ChatHistory history = new ChatHistory();
        history.setUserId(userId);
        history.setRole(role);
        history.setContent(content);
        history.setCreateTime(LocalDateTime.now());
        chatHistoryMapper.insert(history);
    }
}
```

**设计说明**：
- 系统提示词通过 `SysConfigService.getString("ai.chat.system_prompt")` 从 `t_sys_config` 表读取，管理员可在后台修改提示词，无需重启
- 聊天历史按 `user_id` 存储，支持翻页查询
- `chatStream()` 返回 `Flux<String>`，由 Controller 包装为 SSE 流
- `saveAssistantMessage()` 方法由 Controller 在流式响应结束后调用，因为 fluent API 中无法直接获取已消费的完整内容

### 步骤 9：新增实体、Mapper、DTO

**9.1 聊天历史实体**

**新建文件**：`property-module-ai/src/main/java/com/property/module/ai/entity/ChatHistory.java`

```java
package com.property.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天历史记录
 */
@Data
@TableName("t_chat_history")
public class ChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（业主ID） */
    private Long userId;

    /** 角色：user=用户消息，assistant=AI回复 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;
}
```

**9.2 Mapper 接口**

**新建文件**：`property-module-ai/src/main/java/com/property/module/ai/repository/ChatHistoryMapper.java`

```java
package com.property.module.ai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.ai.entity.ChatHistory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天历史 Mapper
 */
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * 按用户ID查询聊天历史（最新在前）
     */
    @Select("SELECT * FROM t_chat_history WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<ChatHistory> selectByUserId(@Param("userId") Long userId,
                                     @Param("offset") int offset,
                                     @Param("size") int size);
}
```

**9.3 请求 DTO**

**新建文件**：`property-module-ai/src/main/java/com/property/module/ai/dto/ChatRequest.java`

```java
package com.property.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 对话请求
 */
@Data
public class ChatRequest {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 用户消息 */
    @NotBlank(message = "消息不能为空")
    private String message;
}
```

**9.4 历史记录 VO**

**新建文件**：`property-module-ai/src/main/java/com/property/module/ai/dto/ChatHistoryVO.java`

```java
package com.property.module.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天历史响应
 */
@Data
public class ChatHistoryVO {

    private Long id;
    private String role;
    private String content;
    private LocalDateTime createTime;
}
```

### 步骤 9.5：注册 Mapper 扫描路径

**文件**：[property-framework/src/main/java/com/property/framework/config/MyBatisPlusConfig.java](file:///d:\.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/config/MyBatisPlusConfig.java)

在 `@MapperScan` 注解的 `basePackages` 数组中新增 `ChatHistoryMapper` 的包路径：

```java
@MapperScan({
        "com.property.framework.repository",
        "com.property.adminapi.repository",
        "com.property.ownerapi.repository",
        "com.property.module.owner.repository",
        "com.property.module.bill.repository",
        "com.property.module.parking.repository",
        "com.property.module.ai.repository"   // ← 新增
})
```

> **说明**：`@MapperScan` 是 MyBatis-Plus 专用的扫描机制，用于为 Mapper 接口生成代理实现 Bean。普通 Java 类（如 `@Service`、`@Component`）由 `@ComponentScan` 负责识别，与此无关。**不添加此配置会导致 `ChatHistoryMapper` 无法注入，启动报错。**

### 步骤 10：新增 ChatController

**新建文件**：`property-owner-api/src/main/java/com/property/ownerapi/controller/ChatController.java`

```java
package com.property.ownerapi.controller;

import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.ai.dto.ChatHistoryVO;
import com.property.module.ai.dto.ChatRequest;
import com.property.module.ai.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 客服接口
 */
@Slf4j
@Tag(name = "AI 客服", description = "AI 物业客服对话接口")
@RestController
@RequestMapping("/api/owner/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiChatService aiChatService;

    @Operation(summary = "流式对话", description = "发送消息并接收 SSE 流式回复（打字机效果）")
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> send(@Valid @RequestBody ChatRequest request) {
        // 从当前登录用户获取 userId（多一层保险，防止传参伪造）
        Long userId = SecurityUtil.getLoginUser().getUserId();
        request.setUserId(userId);

        // 收集 AI 回复的完整内容，用于保存历史
        StringBuilder fullContent = new StringBuilder();

        return aiChatService.chatStream(request)
                .doOnNext(chunk -> {
                    fullContent.append(chunk);
                })
                .doOnComplete(() -> {
                    if (fullContent.length() > 0) {
                        aiChatService.saveAssistantMessage(userId, fullContent.toString());
                    }
                })
                .doOnError(e -> {
                    log.error("SSE 流式对话异常 [userId={}]", userId, e);
                    // 如果已有部分内容，也保存
                    if (fullContent.length() > 0) {
                        aiChatService.saveAssistantMessage(userId,
                                fullContent + "\n[回复中断]");
                    }
                });
    }

    @Operation(summary = "聊天历史", description = "查询当前用户的聊天历史记录")
    @GetMapping("/history")
    public ApiResult<List<ChatHistoryVO>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getLoginUser().getUserId();
        List<ChatHistoryVO> list = aiChatService.getHistory(userId, page, size);
        return ApiResult.success(list);
    }
}
```

**设计说明**：
- `send()` 返回 `Flux<String>` + `produces = TEXT_EVENT_STREAM_VALUE`，浏览器自动识别为 SSE 流
- `userId` 从 `SecurityUtil.getLoginUser()` 获取，**不信任前端传参**，防止越权

---

## 5. 前端改造（步骤 11-13）

### 步骤 11：新增聊天 API 封装

**新建文件**：`property-owner-web/src/api/chat.ts`

```typescript
import request from '@/utils/request'

export interface ChatRequest {
  message: string
}

export interface ChatHistoryVO {
  id: number
  role: string
  content: string
  createTime: string
}

/**
 * 流式对话（SSE）
 * 使用 fetch 而非 axios，因为 axios 不支持流式读取
 */
export function sendMessage(message: string): Promise<Response> {
  return fetch('/api/owner/chat/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify({ message }),
  })
}

/**
 * 查询聊天历史
 */
export function getChatHistory(page = 1, size = 20) {
  return request.get<ChatHistoryVO[]>('/api/owner/chat/history', {
    params: { page, size },
  })
}
```

### 步骤 12：新增聊天页面

**新建文件**：`property-owner-web/src/views/chat/ChatView.vue`

```vue
<template>
  <div class="chat-container">
    <!-- 顶部标题栏 -->
    <div class="chat-header">
      <span class="back-btn" @click="$router.push('/')">&#8592; 返回</span>
      <span class="title">AI 物业客服</span>
      <span class="placeholder"></span>
    </div>

    <!-- 消息列表 -->
    <div class="chat-messages" ref="messagesRef">
      <!-- 欢迎语 -->
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-icon">🤖</div>
        <div class="welcome-text">你好！我是AI智慧社区助手<br/>有什么可以帮您的？</div>
        <div class="quick-questions">
          <span v-for="q in quickQuestions" :key="q" @click="quickSend(q)">{{ q }}</span>
        </div>
      </div>

      <!-- 消息气泡 -->
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['message', msg.role === 'user' ? 'message-user' : 'message-ai']"
      >
        <div class="avatar">
          {{ msg.role === 'user' ? '👤' : '🤖' }}
        </div>
        <div class="bubble">
          <div class="content" v-html="formatContent(msg.content)"></div>
          <div class="time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>

      <!-- 加载中（等待 AI 回复） -->
      <div v-if="loading" class="message message-ai">
        <div class="avatar">🤖</div>
        <div class="bubble">
          <span class="typing"><span></span><span></span><span></span></span>
        </div>
      </div>
    </div>

    <!-- 底部输入框 -->
    <div class="chat-input">
      <input
        v-model="inputText"
        placeholder="输入您的问题..."
        :disabled="loading"
        @keyup.enter="send"
      />
      <button :disabled="loading || !inputText.trim()" @click="send">发送</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { sendMessage, getChatHistory } from '@/api/chat'

interface Message {
  role: string
  content: string
  createTime: string
}

const messages = ref<Message[]>([])
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement | null>(null)

const quickQuestions = [
  '怎么缴纳物业费？',
  '如何报修？',
  '车位怎么租？',
  '本月物业费多少？',
]

onMounted(async () => {
  // 加载历史记录
  try {
    const res = await getChatHistory()
    if (res.code === 200 && res.data) {
      messages.value = res.data.reverse()
      scrollToBottom()
    }
  } catch {
    // 忽略加载失败
  }
})

function scrollToBottom() {
  nextTick(() => {
    const el = messagesRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function quickSend(question: string) {
  inputText.value = question
  send()
}

async function send() {
  if (loading.value) return
  const text = inputText.value.trim()
  if (!text) return

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    createTime: new Date().toISOString(),
  })
  inputText.value = ''
  scrollToBottom()

  // 添加 AI 占位
  loading.value = true
  const aiIdx = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    createTime: new Date().toISOString(),
  })

  try {
    const response = await sendMessage(text)
    if (!response.ok) {
      throw new Error('请求失败')
    }

    // 读取 SSE 流
    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    if (!reader) throw new Error('流不可用')

    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      // 按行解析 SSE 格式
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const chunk = line.substring(5).trim()
          if (chunk) {
            messages.value[aiIdx].content += chunk
            scrollToBottom()
          }
        }
      }
    }
  } catch (e) {
    messages.value[aiIdx].content = '抱歉，回复出了点问题，请稍后重试。'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function formatContent(text: string) {
  return text.replace(/\n/g, '<br/>')
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 600px;
  margin: 0 auto;
  background: #f5f5f5;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;
}
.chat-header .title {
  font-size: 17px;
  font-weight: 600;
}
.chat-header .back-btn {
  color: #1989fa;
  cursor: pointer;
  font-size: 15px;
}
.chat-header .placeholder {
  width: 50px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.welcome {
  text-align: center;
  padding: 40px 0;
}
.welcome-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.welcome-text {
  font-size: 15px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 24px;
}
.quick-questions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}
.quick-questions span {
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #1989fa;
  color: #1989fa;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
}

.message {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}
.message-user {
  flex-direction: row-reverse;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.message-user .avatar {
  margin-left: 10px;
}
.message-ai .avatar {
  margin-right: 10px;
}

.bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
}
.message-user .bubble {
  background: #1989fa;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.message-ai .bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
}
.bubble .time {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.6;
}

/* 打字动画 */
.typing {
  display: inline-flex;
  gap: 4px;
  padding: 4px 0;
}
.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  animation: typing 1.4s infinite;
}
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

.chat-input {
  display: flex;
  padding: 10px 16px;
  background: #fff;
  border-top: 1px solid #eee;
  gap: 10px;
}
.chat-input input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 24px;
  font-size: 15px;
  outline: none;
}
.chat-input input:focus {
  border-color: #1989fa;
}
.chat-input button {
  padding: 10px 20px;
  background: #1989fa;
  color: #fff;
  border: none;
  border-radius: 24px;
  font-size: 15px;
  cursor: pointer;
}
.chat-input button:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
</style>
```

### 步骤 13：注册路由和入口

**13.1 路由**

**文件**：[property-owner-web/src/router/index.ts](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/router/index.ts)

在 `routes` 数组中新增：

```typescript
{
  path: '/chat',
  name: 'chat',
  component: () => import('@/views/chat/ChatView.vue'),
  meta: { requiresAuth: true },
},
```

**13.2 首页入口**

**文件**：[property-owner-web/src/views/home/HomeView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/home/HomeView.vue)

在首页适当位置（如导航栏/功能卡片区）新增 AI 客服入口按钮：

```html
<div class="ai-entry" @click="$router.push('/chat')">
  <span class="ai-icon">🤖</span>
  <span>AI 客服</span>
</div>
```

样式参考：

```css
.ai-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 12px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
}
.ai-icon {
  font-size: 24px;
}
```

---

## 6. 编译与验证

### 6.1 后端编译

```powershell
# 设置 JDK 25 环境
$env:JAVA_HOME="D:\jdk25"

# 编译
mvn -s D:\maven-repo\settings.xml clean compile -DskipTests -q
```

**常见编译问题**：

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Spring AI 依赖拉取失败 | 未配置 Milestones 仓库 | 检查根 pom.xml 是否添加了 `spring-milestones` 仓库 |
| `ChatClient` 找不到 | Spring AI 2.0 包路径变更 | 确认 import 为 `org.springframework.ai.chat.client.ChatClient` |
| `Flux` 找不到 | 缺少 reactor-core 依赖 | Spring AI 已传递依赖 reactor-core，确认依赖树完整 |

### 6.2 启动验证

1. 确保 `DEEPSEEK_API_KEY` 环境变量已设置
2. 启动 `property-owner-api`
3. 确认日志中无 Spring AI 相关错误

### 6.3 功能验证

**测试流式对话（curl）：**

```powershell
# 1. 先登录获取 Cookie
curl -X POST http://localhost:8084/api/owner/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"phone\":\"13800138000\",\"password\":\"123456\",\"captcha\":\"abcd\"}" `
  -c cookies.txt

# 2. 调用流式对话（SSE）
curl -X POST http://localhost:8084/api/owner/chat/send `
  -H "Content-Type: application/json" `
  -b cookies.txt `
  -d "{\"message\":\"怎么缴纳物业费？\"}"
# 预期：返回 SSE 流，逐字输出 AI 回复
```

**测试聊天历史：**

```powershell
curl http://localhost:8084/api/owner/chat/history -b cookies.txt
# 预期：返回 JSON 数组，包含用户和 AI 的历史消息
```

### 6.4 前端验证

1. 启动 owner-web 开发服务器
2. 登录业主端
3. 点击首页「AI 客服」入口
4. 发送消息，观察打字机效果
5. 刷新页面，确认历史记录仍然可见

---

## 7. 验收标准

### 7.1 后端

- [ ] `mvn clean compile -DskipTests` 编译通过，无错误
- [ ] `property-owner-api` 正常启动，Spring AI 自动配置生效
- [ ] `POST /api/owner/chat/send` 返回 SSE 流式响应，打字机效果正常
- [ ] `GET /api/owner/chat/history` 返回历史记录，按时间倒序
- [ ] 未登录调用 `/api/owner/chat/send` 返回 401
- [ ] 修改 `t_sys_config` 中 `ai.chat.system_prompt` 的值后，AI 回答风格立即变化
- [ ] 聊天历史正确写入 `t_chat_history` 表

### 7.2 前端

- [ ] 业主端首页显示「AI 客服」入口
- [ ] 点击入口进入聊天页面，显示欢迎语和快捷问题
- [ ] 发送消息后，AI 回复逐字显示（打字机效果）
- [ ] 多轮对话上下文正常（AI 能理解前文）
- [ ] 刷新页面后聊天历史可见
- [ ] 未登录时访问路由 `/chat` 自动跳转到登录页

### 7.3 回归测试

- [ ] 业主端登录正常
- [ ] 账单查询正常
- [ ] 缴费功能正常
- [ ] 管理端功能不受影响

---

## 附录 A：完整文件变更清单

### 修改文件（9 个）

| # | 文件 | 改动摘要 |
|---|------|---------|
| 1 | `pom.xml` | 新增 `spring-ai.version` 属性、Spring AI BOM 依赖管理、Spring Milestones 仓库、注册 `property-module-ai` 模块 |
| 2 | `property-framework/pom.xml` | 新增 `spring-ai-starter-model-openai` 依赖 |
| 3 | `property-framework/.../config/MyBatisPlusConfig.java` | 新增 `com.property.module.ai.repository` Mapper 扫描路径 |
| 4 | `property-owner-api/pom.xml` | 新增 `property-module-ai` 依赖 |
| 5 | `property-owner-api/src/main/resources/application.yml` | 新增 `spring.ai.openai.*` 配置 |
| 6 | `docker/.env` | 新增 `DEEPSEEK_API_KEY` |
| 7 | `docker/docker-compose.yaml` | owner-api 服务新增 `DEEPSEEK_API_KEY` 环境变量 |
| 8 | `property-owner-web/src/router/index.ts` | 新增 `/chat` 路由 |
| 9 | `property-owner-web/src/views/home/HomeView.vue` | 新增 AI 客服入口 |

### 新增文件（10 个）

| # | 文件 | 用途 |
|---|------|------|
| 1 | `property-module-ai/pom.xml` | AI 模块 Maven 配置 |
| 2 | `property-module-ai/.../config/AiConfig.java` | ChatClient + ChatMemory Bean 配置 |
| 3 | `property-module-ai/.../service/AiChatService.java` | AI 对话业务逻辑 |
| 4 | `property-module-ai/.../entity/ChatHistory.java` | 聊天历史实体 |
| 5 | `property-module-ai/.../repository/ChatHistoryMapper.java` | 聊天历史 Mapper |
| 6 | `property-module-ai/.../dto/ChatRequest.java` | 对话请求 DTO |
| 7 | `property-module-ai/.../dto/ChatHistoryVO.java` | 历史记录 VO |
| 8 | `property-owner-api/.../controller/ChatController.java` | AI 客服接口 |
| 9 | `property-owner-web/src/api/chat.ts` | 前端聊天 API |
| 10 | `property-owner-web/src/views/chat/ChatView.vue` | 聊天页面 |

### 删除文件

无。

---

## 附录 B：聊天历史表结构

```sql
CREATE TABLE `t_chat_history` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID（业主ID）',
    `role` VARCHAR(20) NOT NULL COMMENT '角色：user=用户消息，assistant=AI回复',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天历史记录';
```

## 附录 C：AI 客服对话流程时序

```
业主端 ChatView                  ChatController              AiChatService          DeepSeek API
     │                               │                            │                      │
     │  POST /api/owner/chat/send    │                            │                      │
     │  {message:"怎么缴费？"}        │                            │                      │
     │──────────────────────────────>│                            │                      │
     │                               │                            │                      │
     │                               │  SecurityUtil.getLoginUser │                      │
     │                               │  → userId                  │                      │
     │                               │                            │                      │
     │                               │  chatStream(request)       │                      │
     │                               │───────────────────────────>│                      │
     │                               │                            │                      │
     │                               │                            │  sysConfigService    │
     │                               │                            │  .getString("ai.     │
     │                               │                            │   chat.system_prompt")│
     │                               │                            │                      │
     │                               │                            │  saveHistory(user)    │
     │                               │                            │  → t_chat_history     │
     │                               │                            │                      │
     │                               │                            │  chatClient.prompt() │
     │                               │                            │  .system(prompt)     │
     │                               │                            │  .user(message)      │
     │                               │                            │  .stream().content() │
     │                               │                            │─────────────────────>│
     │                               │                            │                      │
     │                               │                            │  SSE: data: 您     │
     │                               │                            │<─────────────────────│
     │                               │    Flux<String>            │                      │
     │                               │<───────────────────────────│                      │
     │                               │                            │                      │
     │  SSE: data: 您可以通...        │                            │                      │
     │<──────────────────────────────│                            │                      │
     │                               │                            │                      │
     │  SSE: data: 过物业...          │                            │                      │
     │<──────────────────────────────│                            │                      │
     │                               │                            │                      │
     │  SSE: data: [DONE]            │                            │                      │
     │<──────────────────────────────│                            │                      │
     │                               │                            │                      │
     │                               │  saveAssistantMessage(     │                      │
     │                               │    userId, fullContent)    │                      │
     │                               │───────────────────────────>│                      │
     │                               │                            │  → t_chat_history     │
     │                               │                            │                      │
```