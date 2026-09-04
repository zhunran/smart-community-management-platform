# AI 功能修复指南（RAG 搁置）

> 状态：待实施
> 目标：移除 RAG（向量库/检索增强）相关依赖，使 AI 流式对话功能正常启动运行
> 影响模块：`property-module-ai`、`property-owner-api`、`property-admin-api`

---

## 1. 问题根因

当前 `AiConfig` 中 `chatClient` 强依赖 `VectorStore`（用于 `QuestionAnswerAdvisor`），但实际业务中 `AiChatService` 仅使用函数调用工具（`CommunityInfoTool`），不涉及 RAG。同时：

- `property-owner-api` 配置了 `embedding.enabled: false`，导致无 `EmbeddingModel` Bean
- `AiConfig` 的 `SimpleVectorStore` 兜底需要 `EmbeddingModel`，创建失败

最终 `chatClient` 找不到 `VectorStore` → 启动失败。

---

## 2. 修复清单

### 2.1 模块依赖精简（`property-module-ai/pom.xml`）

**删除** 2 个 RAG 相关依赖，保留核心 AI 对话能力：

```xml
<!-- 删除以下两个依赖 -->
<!-- Spring AI Redis 向量存储（RAG 可选） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-redis</artifactId>
</dependency>
<!-- RAG 检索增强 Advisor（QuestionAnswerAdvisor） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vector-store-advisor</artifactId>
</dependency>
```

保留的依赖（无需改动）：
| 依赖 | 用途 |
|------|------|
| `spring-ai-starter-model-openai` | 核心：OpenAI 协议模型调用（DeepSeek） |
| `property-framework` | 基础框架 |
| `property-module-bill/owner/parking/notification` | AI 工具类查询业务数据 |

---

### 2.2 精简 `AiConfig`（`property-module-ai/.../config/AiConfig.java`）

**目标**：移除 `VectorStore` 和 `QuestionAnswerAdvisor`，仅保留 `MessageChatMemoryAdvisor`（对话记忆）。

替换为：

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
 * ChatClient 用于 SSE 流式对话，ChatMemory 由 Spring AI 自动配置提供。
 * 当前仅保留对话记忆 Advisor，RAG 相关（QuestionAnswerAdvisor / VectorStore）留待后续阶段引入。
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
```

**删除的内容**：
| 删除项 | 原因 |
|--------|------|
| `vectorStore(EmbeddingModel)` Bean | RAG 搁置，不需要 VectorStore |
| `QuestionAnswerAdvisor` | RAG 搁置，不需要检索增强 |
| `VectorStore` / `EmbeddingModel` / `SimpleVectorStore` / `ConditionalOnMissingBean` import | 不再使用 |

---

### 2.3 精简 `application.yml`（`property-owner-api`）

**删除** 以下 RAG 相关配置：

```yaml
# 删除以下三行（vectorstore 占位 + embedding 禁用）
vectorstore:
  type: none
embedding:
  enabled: false
```

替换后 `spring.ai` 配置段为：

```yaml
  # Spring AI 配置（DeepSeek，OpenAI 兼容协议）
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        temperature: 0.7
        model: deepseek-v4-flash
```

---

### 2.4 精简 `application.yml`（`property-admin-api`）

**删除** `vectorstore.type: none` 占位配置（不再需要）：

```yaml
# 删除此段
vectorstore:
  type: none
```

替换后 `spring.ai` 配置段为：

```yaml
  # Spring AI 相关：为管理员端预留 AI 基础设施（当前未实现具体功能），
  # 配置 api-key 环境变量使 OpenAI 模型客户端正常初始化，避免启动失败。
  # 后续如需补充 AI 功能，直接基于此配置扩展即可。
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        temperature: 0.7
        model: deepseek-v4-flash
```

---

## 3. 影响范围总结

| 文件 | 操作 | 说明 |
|------|------|------|
| `property-module-ai/pom.xml` | 删 2 个依赖 | 移除 `spring-ai-starter-vector-store-redis`、`spring-ai-vector-store-advisor` |
| `property-module-ai/.../config/AiConfig.java` | 重写 | 删 VectorStore Bean、QuestionAnswerAdvisor，chatClient 仅保留 MessageChatMemoryAdvisor |
| `property-owner-api/.../application.yml` | 删 3 行 | 移除 `vectorstore.type`、`embedding.enabled` |
| `property-admin-api/.../application.yml` | 删 2 行 | 移除 `vectorstore.type` |

**不影响的功能**：
- `AiChatService` 流式对话（SSE）—— 不变
- `ChatController` —— 不变
- `AiToolConfig` + `CommunityInfoTool` 函数调用 —— 不变
- `ChatHistory` 聊天历史存储 —— 不变
- 对话记忆（`ChatMemory` + `MessageChatMemoryAdvisor`）—— 不变

---

## 4. 后续 RAG 重新引入时

当需要恢复 RAG 功能时，按以下步骤反向操作：

1. `pom.xml` 重新加回 `spring-ai-starter-vector-store-redis`、`spring-ai-vector-store-advisor`
2. `AiConfig` 恢复 `vectorStore` Bean + `QuestionAnswerAdvisor`
3. `application.yml` 恢复 `spring.ai.openai.embedding.enabled: true` 和向量库配置
4. 准备知识库文档，导入向量库

---

## 5. 验证步骤

```bash
# 1. 编译 AI 模块
cd property-module-ai && mvn clean compile

# 2. 启动 owner-api（需先配置环境变量 DEEPSEEK_API_KEY）
cd ../property-owner-api && mvn spring-boot:run

# 3. 验证 AI 对话接口
curl -X POST http://localhost:8084/api/owner/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"你好"}'
# 预期：返回 SSE 流式响应，打字机效果
```

---

> 编写日期：2026-08-22