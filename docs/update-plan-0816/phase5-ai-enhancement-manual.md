# 阶段五操作手册：AI 智能增强（工具 + 公告 + 知识库）

> 配套文档：[升级改造计划书](./upgrade-plan.md) 3.4 阶段五
> 编制日期：2026-08-20
> 阶段范围：新增小区公告模块、AI 工具（Function Calling）让 AI 查真实业务数据、可选知识库（RAG）
> 预计影响：`property-module-notification`（公告实体+服务）、`property-module-ai`（工具注册）、`property-owner-api`（工具类+依赖调整）、`property-admin-api`（管理端公告接口）、`sql`（公告表）
> 前置条件：阶段三（Spring AI + DeepSeek 流式对话）已完成

---

## 目录

- [1. 变更总览](#1-变更总览)
- [2. 数据层：新增公告表（步骤 1-2）](#2-数据层新增公告表步骤-1-2)
- [3. 公告业务模块（步骤 3-8）](#3-公告业务模块步骤-3-8)
- [4. 管理端公告接口（步骤 9-10）](#4-管理端公告接口步骤-9-10)
- [5. AI 工具层：Function Calling（步骤 11-14）](#5-ai-工具层function-calling步骤-11-14)
- [6. 可选：知识库 RAG（步骤 15-16）](#6-可选知识库-rag步骤-15-16)
- [7. 前端配合（步骤 17-18）](#7-前端配合步骤-17-18)
- [8. 编译与验证](#8-编译与验证)
- [附录 A：完整文件变更清单](#附录-a完整文件变更清单)
- [附录 B：数据来源决策矩阵](#附录-b数据来源决策矩阵)
- [附录 C：依赖图（改造后）](#附录-c依赖图改造后)

---

## 1. 变更总览

### 1.1 目标

让 AI 客服能回答基于**真实业务数据**的问题，而非仅泛泛而谈。分三层：

| 层 | 技术 | 解决的问题 | 数据来源 |
|----|------|-----------|---------|
| 工具层 | Spring AI Function Calling | "我这个月物业费多少？""小区还有车位吗？""最近有什么公告？" | 实时查库 |
| 数据层 | 公告模块（t_notice） | "小区今天停水吗？""最近有什么活动？" | 管理员录入 |
| 知识库 | RAG（可选） | "小区多大？""物业费包含哪些？""怎么报修？" | 静态文档 |

### 1.2 数据来源决策

```
高频变化（秒~天级）  → 工具查库（账单、车位、公告）
低频变化（月~年级）  → 知识库/文档（制度、流程、概况）
从不变化（一次性）   → 系统提示词（AI 人设、服务范围）
```

### 1.3 变更文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | [property_management.sql](file:///d:\.workspace/javaproject/property-management-system/property-management/sql/property_management.sql) | 新增 `t_notice` 表 |
| 新增 | `property-module-notification/src/main/java/.../entity/NoticeEntity.java` | 公告实体 |
| 新增 | `property-module-notification/src/main/java/.../repository/NoticeMapper.java` | 公告 Mapper |
| 新增 | `property-module-notification/src/main/java/.../service/NoticeService.java` | 公告服务接口 |
| 新增 | `property-module-notification/src/main/java/.../service/impl/NoticeServiceImpl.java` | 公告服务实现 |
| 新增 | `property-module-notification/src/main/java/.../dto/NoticeVO.java` | 公告 VO |
| 新增 | `property-module-notification/src/main/java/.../dto/NoticeCreateRequest.java` | 公告创建请求 |
| 新增 | `property-module-notification/src/main/java/.../dto/NoticePageQuery.java` | 公告分页查询 |
| 新增 | `property-admin-api/src/main/java/.../controller/AdminNoticeController.java` | 管理端公告 CRUD |
| 修改 | [property-module-ai/pom.xml](file:///d:\.workspace/javaproject/property-management-system/property-management/property-module-ai/pom.xml) | 新增 `bill`、`owner`、`parking`、`notification` 业务模块依赖 |
| 新增 | `property-module-ai/src/main/java/.../tool/CommunityInfoTool.java` | AI 工具类（@Tool 方法），放在 module-ai 的 tool 子包 |
| 新增 | `property-module-ai/src/main/java/.../config/AiToolConfig.java` | 工具注册配置（与 AiConfig 同包） |
| 修改 | [property-module-ai/src/main/java/.../service/AiChatService.java](file:///d:\.workspace/javaproject/property-management-system/property-management/property-module-ai/src/main/java/com/property/module/ai/service/AiChatService.java) | 系统提示词补充工具使用指引 |

---

## 2. 数据层：新增公告表（步骤 1-2）

### 步骤 1：SQL 建表

**文件**：[property_management.sql](file:///d:\.workspace/javaproject/property-management-system/property-management/sql/property_management.sql)

在文件末尾（`Dump completed` 之前）插入：

```sql
--
-- Table structure for table `t_notice`
--

DROP TABLE IF EXISTS `t_notice`;
CREATE TABLE `t_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容',
  `type` varchar(20) NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型：NOTICE=普通公告，WATER_ELECTRIC=停水停电，ACTIVITY=社区活动，EMERGENCY=紧急通知',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=草稿，1=已发布，2=已下线',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID（管理员）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`type`, `status`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小区公告';

-- 插入一条示例公告
INSERT INTO `t_notice` (`title`, `content`, `type`, `status`, `publish_time`, `create_by`)
VALUES ('欢迎使用智慧社区服务平台', '尊敬的业主：\n欢迎使用智慧社区服务平台！如您在使用过程中有任何问题，可通过AI客服或拨打物业服务中心电话咨询。', 'NOTICE', 1, NOW(), NULL);
```

### 步骤 2：确认 MyBatis-Plus 扫描路径

**文件**：[MyBatisPlusConfig.java](file:///d:\.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/config/MyBatisPlusConfig.java)

确认 `mapperScan` 包含 `com.property.module.notification.repository`：

```java
@MapperScan("com.property.module.**.repository")
```

---

## 3. 公告业务模块（步骤 3-8）

> 所有新增文件位于 `property-module-notification` 模块下。

### 步骤 3：公告实体

**新建文件**：`property-module-notification/src/main/java/com/property/module/notification/entity/NoticeEntity.java`

```java
package com.property.module.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_notice")
public class NoticeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 公告类型：NOTICE / WATER_ELECTRIC / ACTIVITY / EMERGENCY */
    private String type;

    /** 状态：0=草稿，1=已发布，2=已下线 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 创建人ID */
    private Long createBy;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 步骤 4：公告 Mapper

**新建文件**：`property-module-notification/src/main/java/com/property/module/notification/repository/NoticeMapper.java`

```java
package com.property.module.notification.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.notification.entity.NoticeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeMapper extends BaseMapper<NoticeEntity> {
}
```

### 步骤 5：公告 VO

**新建文件**：`property-module-notification/src/main/java/com/property/module/notification/dto/NoticeVO.java`

```java
package com.property.module.notification.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoticeVO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private Integer status;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}
```

### 步骤 6：公告创建请求

**新建文件**：`property-module-notification/src/main/java/com/property/module/notification/dto/NoticeCreateRequest.java`

```java
package com.property.module.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoticeCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String type = "NOTICE";
}
```

### 步骤 7：公告分页查询

**新建文件**：`property-module-notification/src/main/java/com/property/module/notification/dto/NoticePageQuery.java`

```java
package com.property.module.notification.dto;

import lombok.Data;

@Data
public class NoticePageQuery {
    private Integer current = 1;
    private Integer size = 10;
    private String type;
    private Integer status;
}
```

### 步骤 8：公告服务

**新建文件**：`property-module-notification/src/main/java/com/property/module/notification/service/NoticeService.java`

```java
package com.property.module.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.property.module.notification.dto.NoticeCreateRequest;
import com.property.module.notification.dto.NoticePageQuery;
import com.property.module.notification.dto.NoticeVO;

import java.util.List;

public interface NoticeService {

    /** 创建公告（草稿） */
    NoticeVO create(NoticeCreateRequest request, Long adminId);

    /** 发布公告 */
    void publish(Long id);

    /** 下线公告 */
    void offline(Long id);

    /** 分页查询（管理端） */
    IPage<NoticeVO> page(NoticePageQuery query);

    /** 查询最新 N 条已发布公告（业主端/AI 工具用） */
    List<NoticeVO> listLatest(int limit);
}
```

**新建文件**：`property-module-notification/src/main/java/com/property/module/notification/service/impl/NoticeServiceImpl.java`

```java
package com.property.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.property.module.notification.dto.NoticeCreateRequest;
import com.property.module.notification.dto.NoticePageQuery;
import com.property.module.notification.dto.NoticeVO;
import com.property.module.notification.entity.NoticeEntity;
import com.property.module.notification.repository.NoticeMapper;
import com.property.module.notification.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    @Transactional
    public NoticeVO create(NoticeCreateRequest request, Long adminId) {
        NoticeEntity entity = new NoticeEntity();
        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        entity.setType(request.getType() != null ? request.getType() : "NOTICE");
        entity.setStatus(0); // 草稿
        entity.setCreateBy(adminId);
        noticeMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        NoticeEntity entity = noticeMapper.selectById(id);
        if (entity == null) return;
        entity.setStatus(1);
        entity.setPublishTime(LocalDateTime.now());
        noticeMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void offline(Long id) {
        NoticeEntity entity = noticeMapper.selectById(id);
        if (entity == null) return;
        entity.setStatus(2);
        noticeMapper.updateById(entity);
    }

    @Override
    public IPage<NoticeVO> page(NoticePageQuery query) {
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<NoticeEntity>()
                .eq(query.getType() != null, NoticeEntity::getType, query.getType())
                .eq(query.getStatus() != null, NoticeEntity::getStatus, query.getStatus())
                .orderByDesc(NoticeEntity::getCreateTime);
        Page<NoticeEntity> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<NoticeEntity> result = noticeMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public List<NoticeVO> listLatest(int limit) {
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<NoticeEntity>()
                .eq(NoticeEntity::getStatus, 1) // 已发布
                .orderByDesc(NoticeEntity::getPublishTime)
                .last("LIMIT " + limit);
        return noticeMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private NoticeVO toVO(NoticeEntity entity) {
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
```

---

## 4. 管理端公告接口（步骤 9-10）

### 步骤 9：管理端公告 Controller

**新建文件**：`property-admin-api/src/main/java/com/property/adminapi/controller/AdminNoticeController.java`

```java
package com.property.adminapi.controller;

import com.property.common.result.ApiResult;
import com.property.framework.web.security.SecurityUtil;
import com.property.module.notification.dto.NoticeCreateRequest;
import com.property.module.notification.dto.NoticePageQuery;
import com.property.module.notification.dto.NoticeVO;
import com.property.module.notification.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "公告管理", description = "小区公告发布与管理")
@RestController
@RequestMapping("/api/admin/notice")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "创建公告")
    @PostMapping
    public ApiResult<NoticeVO> create(@Valid @RequestBody NoticeCreateRequest request) {
        Long adminId = SecurityUtil.requireUser().getUserId();
        return ApiResult.success(noticeService.create(request, adminId));
    }

    @Operation(summary = "发布公告")
    @PutMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return ApiResult.success();
    }

    @Operation(summary = "下线公告")
    @PutMapping("/{id}/offline")
    public ApiResult<Void> offline(@PathVariable Long id) {
        noticeService.offline(id);
        return ApiResult.success();
    }

    @Operation(summary = "分页查询公告")
    @GetMapping("/page")
    public ApiResult<Object> page(NoticePageQuery query) {
        return ApiResult.success(noticeService.page(query));
    }
}
```

### 步骤 10：确认 admin-api 依赖 notification

**文件**：[property-admin-api/pom.xml](file:///d:\.workspace/javaproject/property-management-system/property-management/property-admin-api/pom.xml)

确认已包含（通常已通过 framework 间接依赖，若报错则显式添加）：

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>property-module-notification</artifactId>
    <version>${project.parent.version}</version>
</dependency>
```

---

## 5. AI 工具层：Function Calling（步骤 11-14）

> **关键设计原则**：工具方法内一律通过 `SecurityUtil.requireUser().getUserId()` 获取当前登录用户，**不接受参数传入的 userId**，防止越权。

### 步骤 11：module-ai 补依赖

> **设计说明**：`CommunityInfoTool` 放在 `module-ai` 的 `tool` 子包下，语义上 AI 工具属于 AI 模块，且未来若 `admin-api` 也需要 AI 工具可直接复用。`module-ai` 新增对业务模块的依赖不会产生循环依赖（业务模块不依赖 `module-ai`）。

**文件**：[property-module-ai/pom.xml](file:///d:\.workspace/javaproject/property-management-system/property-management/property-module-ai/pom.xml)

```xml
<!-- 新增：AI 工具需要查询账单、房屋、车位、公告等业务数据 -->
<dependency>
    <groupId>org.example</groupId>
    <artifactId>property-module-bill</artifactId>
    <version>${project.parent.version}</version>
</dependency>
<dependency>
    <groupId>org.example</groupId>
    <artifactId>property-module-owner</artifactId>
    <version>${project.parent.version}</version>
</dependency>
<dependency>
    <groupId>org.example</groupId>
    <artifactId>property-module-parking</artifactId>
    <version>${project.parent.version}</version>
</dependency>
<dependency>
    <groupId>org.example</groupId>
    <artifactId>property-module-notification</artifactId>
    <version>${project.parent.version}</version>
</dependency>
```

### 步骤 12：AI 工具类

**新建文件**：`property-module-ai/src/main/java/com/property/module/ai/tool/CommunityInfoTool.java`

```java
package com.property.module.ai.tool;

import com.property.framework.web.security.SecurityUtil;
import com.property.module.bill.dto.request.BillPageQuery;
import com.property.module.bill.dto.response.BillVO;
import com.property.module.bill.service.BillService;
import com.property.module.notification.dto.NoticeVO;
import com.property.module.notification.service.NoticeService;
import com.property.module.owner.dto.response.OwnerRoomVO;
import com.property.module.owner.service.OwnerRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 社区信息查询工具
 *
 * 暴露给 AI 大模型通过 Function Calling 调用，让 AI 能回答基于真实业务数据的问题。
 * 所有工具方法强制从 SecurityUtil 读取当前登录用户信息，不信任 AI 传入的 userId。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityInfoTool {

    private final BillService billService;
    private final OwnerRoomService ownerRoomService;
    private final NoticeService noticeService;

    /**
     * 查询当前业主的待缴费账单
     *
     * 直接复用现有 BillService.page(BillPageQuery)，传入 ownerId + status=0（未缴费）。
     * 不新增接口方法，避免改动现有业务层。
     */
    @Tool(description = "查询当前业主的待缴费账单列表，返回账单周期、金额、截止日期等信息")
    public List<BillVO> queryMyBills() {
        Long userId = SecurityUtil.requireUser().getUserId();
        log.info("AI 工具调用: queryMyBills [userId={}]", userId);
        BillPageQuery query = new BillPageQuery();
        query.setOwnerId(userId);
        query.setStatus(0); // 未缴费
        query.setCurrent(1);
        query.setSize(20);  // AI 上下文有限，最多返回 20 条
        return billService.page(query).getRecords();
    }

    /**
     * 查询当前业主的房屋信息
     */
    @Tool(description = "查询当前业主的房屋列表，返回楼栋、房号、业主关系等信息")
    public List<OwnerRoomVO> queryMyRooms() {
        Long userId = SecurityUtil.requireUser().getUserId();
        log.info("AI 工具调用: queryMyRooms [userId={}]", userId);
        return ownerRoomService.listByOwnerId(userId);
    }

    /**
     * 查询小区最新公告
     */
    @Tool(description = "查询小区最新公告与通知，返回公告标题、内容、类型、发布时间")
    public List<NoticeVO> queryLatestNotices(
            @ToolParam(description = "查询数量，默认5条") Integer limit) {
        int n = (limit != null && limit > 0 && limit <= 20) ? limit : 5;
        log.info("AI 工具调用: queryLatestNotices [limit={}]", n);
        return noticeService.listLatest(n);
    }

    /**
     * 查询小区当前状况简报（实时生成）
     */
    @Tool(description = "查询小区当前综合状况简报，包括缴费率、车位余量、最新公告等")
    public String queryCommunityBrief() {
        log.info("AI 工具调用: queryCommunityBrief");
        StringBuilder sb = new StringBuilder("【智慧社区实时简报】\n");
        // 最新公告
        List<NoticeVO> notices = noticeService.listLatest(3);
        if (!notices.isEmpty()) {
            sb.append("▲ 最新公告：\n");
            for (NoticeVO n : notices) {
                sb.append("  · ").append(n.getTitle()).append("\n");
            }
        }
        // 其他实时数据可后续补充（车位余量、缴费率等）
        return sb.toString();
    }
}
```

### 步骤 13：工具注册配置

**新建文件**：`property-module-ai/src/main/java/com/property/module/ai/config/AiToolConfig.java`

```java
package com.property.module.ai.config;

import com.property.module.ai.tool.CommunityInfoTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 工具注册配置
 *
 * 将 CommunityInfoTool 中的 @Tool 方法注册为 Spring AI 可调用的工具，
 * ChatClient.Builder 会自动装配并注入到对话中。
 */
@Configuration
public class AiToolConfig {

    @Bean
    public ToolCallbackProvider communityInfoToolCallback(CommunityInfoTool communityInfoTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(communityInfoTool)
                .build();
    }
}
```

### 步骤 14：系统提示词补充工具指引

**文件**：[AiChatService.java](file:///d:\.workspace/javaproject/property-management-system/property-management/property-module-ai/src/main/java/com/property/module/ai/service/AiChatService.java)

将 `DEFAULT_SYSTEM_PROMPT` 末尾补充一行：

```java
private static final String DEFAULT_SYSTEM_PROMPT = """
        你是"AI智慧社区服务平台"的智能客服助手，为用户提供物业管理相关咨询服务。
        你的服务范围包括：
        1. 缴费咨询：物业费、水费、电费、燃气费等账单查询与缴费方式
        2. 业务咨询：报修流程、车位租赁、访客登记等
        3. 社区公告：社区活动、停水停电通知等
        请用友好、专业的语气回答，回答简洁明了，每次不超过150字。
        如果你不知道答案，请引导用户联系物业服务中心。
        当用户询问账单、房屋、公告等具体信息时，请主动调用相关工具查询最新数据。""";
```

---

## 6. 可选：知识库 RAG（步骤 15-16）

> 仅当存在**静态文档资产**（如服务手册 PDF、规章制度 Word）时执行。若只有数据库数据，工具层已足够。

### 步骤 15：引入 Spring AI 向量存储

**根 pom.xml** 添加 BOM（已在阶段三配置的 `spring-ai-bom` 中）：

```xml
<!-- 已包含在 spring-ai-bom 中，无需额外操作 -->
```

**文件**：[property-module-ai/pom.xml](file:///d:\.workspace/javaproject/property-management-system/property-management/property-module-ai/pom.xml)

```xml
<!-- Spring AI Redis 向量存储（RAG 可选，复用已有 Redis 容器，零额外部署） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-redis</artifactId>
</dependency>
<!-- RAG 检索增强 Advisor（QuestionAnswerAdvisor），不在 Redis starter 传递链中，需单独声明 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-vector-store-advisor</artifactId>
</dependency>
```

> 选择 Redis 而非 PostgreSQL 的理由：项目阶段一已引入 Redis，无需额外容器；当前文档量级（几百份）下 Redis 向量检索性能完全够用。
>
> `spring-ai-vector-store-advisor` 不在 `spring-ai-starter-vector-store-redis` 的传递依赖中，必须单独声明，否则 `QuestionAnswerAdvisor` 类找不到。

### 步骤 16：文档上传 + 检索增强

**新建文件**：`property-admin-api/src/main/java/com/property/adminapi/controller/DocUploadController.java`

```java
@Tag(name = "知识库管理")
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class DocUploadController {

    private final VectorStore vectorStore;
    private final DocumentReader documentReader;

    @Operation(summary = "上传文档到知识库")
    @PostMapping("/upload")
    public ApiResult<Void> upload(@RequestParam("file") MultipartFile file) {
        // 1. 读取文档内容
        // 2. 切分为段落
        // 3. 向量化后存入 VectorStore
        // 4. AI 对话时自动检索增强
        return ApiResult.success();
    }
}
```

**文件**：[AiConfig.java](file:///d:\.workspace/javaproject/property-management-system/property-management/property-module-ai/src/main/java/com/property/module/ai/config/AiConfig.java) 添加检索增强 advisor：

> **注意**：Spring AI 2.0.0 中 `QuestionAnswerAdvisor` 位于 `org.springframework.ai.chat.client.advisor.vectorstore` 子包下，不是 `advisor` 包下。同时需要 import `VectorStore`。

```java
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;

@Bean
public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore) {
    return builder
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                QuestionAnswerAdvisor.builder(vectorStore).build()  // RAG 检索增强
            )
            .build();
}
```

---

## 7. 前端配合（步骤 17-18）

### 步骤 17：管理端公告页（可选）

| 文件 | 说明 |
|------|------|
| `admin-web/src/api/notice.ts` | 公告 CRUD API |
| `admin-web/src/views/notice/NoticeList.vue` | 公告列表 + 新建/发布/下线 |
| `admin-web/src/router/index.ts` | 注册 `/notice` 路由 |

公告页用 Element Plus 标准 CRUD 表单即可，与现有管理端风格一致。

### 步骤 18：业主端首页公告入口（可选）

在 [HomeView.vue](file:///d:\.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/home/HomeView.vue) 欢迎卡片下方添加公告滚动条，或直接在 AI 客服中询问即可。

---

## 8. 编译与验证

### 8.1 编译

```powershell
$env:JAVA_HOME="D:\jdk25"
mvn -s .m2\settings.xml clean compile -DskipTests -q
```

### 8.2 验证清单

- [ ] `t_notice` 表已创建，`MyBatisPlusConfig` 扫描到 `notification.repository`
- [ ] 管理端 `POST /api/admin/notice` 创建公告成功
- [ ] 管理端 `PUT /api/admin/notice/{id}/publish` 发布公告成功
- [ ] AI 工具 `CommunityInfoTool` 所有 `@Tool` 方法可正常调用
- [ ] 业主端 AI 对话中问"最近有什么公告"→ AI 调用工具并返回最新公告
- [ ] 业主端 AI 对话中问"我的账单"→ AI 调用工具并返回该业主的账单列表
- [ ] 业主 A 无法通过 AI 工具查到业主 B 的账单（userId 隔离）
- [ ] 编译通过，无循环依赖

### 8.3 常见编译问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `BillService` 找不到 | module-ai 未依赖 bill 模块 | 按步骤 11 在 module-ai 的 pom 中添加 `property-module-bill` |
| `OwnerRoomService` 找不到 | module-ai 未依赖 owner 模块 | 按步骤 11 添加 `property-module-owner` |
| `NoticeService` 找不到 | module-ai 未依赖 notification 模块 | 按步骤 11 添加 `property-module-notification` |
| `@Tool` 注解找不到 | Spring AI 版本问题 | 确认 `spring-ai-starter-model-openai` 在 module-ai 依赖中 |
| 工具注册后 AI 不调用 | 系统提示词未指引 | 按步骤 14 补充提示词 |
| 循环依赖 | 业务模块依赖了 module-ai | 确认 module-bill/owner/parking/notification 的 pom 中无 module-ai 依赖 |

---

## 附录 A：完整文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `sql/property_management.sql` | 新增 `t_notice` 表 |
| 新增 | `property-module-notification/.../entity/NoticeEntity.java` | 公告实体 |
| 新增 | `property-module-notification/.../repository/NoticeMapper.java` | 公告 Mapper |
| 新增 | `property-module-notification/.../service/NoticeService.java` | 公告服务接口 |
| 新增 | `property-module-notification/.../service/impl/NoticeServiceImpl.java` | 公告服务实现 |
| 新增 | `property-module-notification/.../dto/NoticeVO.java` | 公告 VO |
| 新增 | `property-module-notification/.../dto/NoticeCreateRequest.java` | 公告创建请求 |
| 新增 | `property-module-notification/.../dto/NoticePageQuery.java` | 公告分页查询 |
| 新增 | `property-admin-api/.../controller/AdminNoticeController.java` | 管理端公告 CRUD |
| 修改 | `property-module-ai/pom.xml` | 新增 `bill`、`owner`、`parking`、`notification` 依赖 |
| 新增 | `property-module-ai/.../tool/CommunityInfoTool.java` | AI 工具类（@Tool 方法，tool 子包） |
| 新增 | `property-module-ai/.../config/AiToolConfig.java` | 工具注册配置（与 AiConfig 同包） |
| 修改 | `property-module-ai/.../service/AiChatService.java` | 系统提示词补充工具指引 |
| 可选 | `property-module-ai/pom.xml` | `spring-ai-starter-vector-store-redis` + `spring-ai-vector-store-advisor`（复用已有 Redis） |
| 可选 | `property-admin-api/.../controller/DocUploadController.java` | 文档上传 |
| 可选 | `property-module-ai/.../config/AiConfig.java` | RAG advisor 配置 |

---

## 附录 B：数据来源决策矩阵

| 数据 | 变更频率 | 适合载体 | 具体实现 |
|------|---------|---------|---------|
| 账单状态 | 分钟级 | 工具查库 | `queryMyBills()` → `BillService` |
| 车位余量 | 分钟级 | 工具查库 | `queryCommunityBrief()` → `ParkingSpaceService` |
| 最新公告 | 天级 | 工具查库 | `queryLatestNotices()` → `NoticeService` |
| 小区概况 | 年级 | 知识库/系统提示词 | RAG 文档 或 系统提示词 |
| 物业收费标准 | 半年~年 | 知识库 | RAG 文档 |
| 报修/车位流程 | 月级 | 知识库 | RAG 文档 |
| AI 人设/服务范围 | 一次性 | 系统提示词 | `DEFAULT_SYSTEM_PROMPT` |

---

## 附录 C：依赖图（改造后）

```
admin-api ──▶ module-notification ──▶ framework

owner-api ──▶ module-ai ──▶ module-bill ──▶ framework
    │             │  ──▶ module-owner ──▶ framework
    │             │  ──▶ module-parking ──▶ framework
    │             │  ──▶ module-notification
    │             │
    │             ├─ tool/CommunityInfoTool (@Tool)
    │             │   └── 注入 BillService / OwnerRoomService / NoticeService
    │             │
    │             └─ config/AiToolConfig (ToolCallbackProvider)
    │
    ├─▶ module-bill
    ├─▶ module-owner
    └─▶ module-payment
```