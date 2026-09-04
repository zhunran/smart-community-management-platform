# 社区互动模块（property-module-community）设计

> 职责：社区活动、邻里论坛、投票问卷。
> 依赖：property-framework（复用鉴权/异常/统一响应/操作日志/RedisUtil）。
> 不依赖任何其他业务模块；需要房屋信息时通过 `RoomDataService` 接口调用（housing 模块提供）。

---

## 一、模块骨架

```
property-module-community/
├─ pom.xml                          ← 依赖 framework + lombok + mapstruct(可选)
└─ src/main/java/com/property/module/community/
    ├─ controller/                  ← 仅业主端只读接口写在此处可选；管理端写操作统一放 admin-api
    │   （本模块不建 Controller，与 housing 模块同策略，由入口层聚合）
    ├─ entity/
    │   ├─ CommunityActivityEntity.java
    │   ├─ ActivitySignupEntity.java
    │   ├─ ForumPostEntity.java
    │   ├─ ForumCommentEntity.java
    │   └─ ForumLikeEntity.java
    ├─ enums/                        ← 枚举统一放 property-common（ActivityStatusEnum 等），本模块不单独维护
    ├─ repository/
    │   ├─ CommunityActivityMapper.java
    │   ├─ ActivitySignupMapper.java
    │   ├─ ForumPostMapper.java
    │   ├─ ForumCommentMapper.java
    │   └─ ForumLikeMapper.java
    ├─ service/
    │   ├─ CommunityActivityService.java
    │   ├─ ForumService.java
    │   └─ impl/
    │       ├─ CommunityActivityServiceImpl.java
    │       └─ ForumServiceImpl.java
    ├─ converter/
    │   ├─ ActivityConverter.java        ← @Mapper(componentModel="spring")
    │   └─ ForumConverter.java
    └─ dto/
        ├─ activity/  (ActivityCreateRequest, ActivityUpdateRequest, ActivityVO, ActivityDetailVO, SignupRequest)
        └─ forum/     (PostCreateRequest, PostVO, PostDetailVO, CommentCreateRequest, CommentVO)
```

**注册步骤（勿漏，踩过坑）**：

1. 根 `pom.xml` `<modules>` 加 `<module>property-module-community</module>`
2. `admin-api`、`owner-api` 的 pom 加依赖
3. `framework/MyBatisPlusConfig` 的 `@MapperScan` 加 `com.property.module.community.repository`
4. 启动类 `scanBasePackages` 已含 `com.property.module`（通配）则无需改；若为枚举式需补 `com.property.module.community`
5. **子模块 pom 必须显式声明 `maven-compiler-plugin`**（继承 pluginManagement 的注解处理器链），mapstruct 依赖 `<scope>provided</scope>` —— 教训见 `docs/problem.md` 2026-08-30 条目

---

## 二、社区活动

### 2.1 状态机

```
草稿(0) --发布--> 招募中(1) --满员--> 已满员(2) --到达开始时间--> 进行中(3) --到达结束时间--> 已结束(4)
                     │                                                        ↑
                     └──────────── 取消(5) ←── 任意未开始状态 ────────────────┘
```

状态流转触发方：

| 流转           | 触发方   | 实现                                                     |
| -------------- | -------- | -------------------------------------------------------- |
| 草稿→招募中    | 管理端   | `AdminActivityController.publish()`                      |
| 招募中→已满员  | 报名时   | 报名事务内 `signup_count >= max_participants` 时顺带更新 |
| →进行中/已结束 | 定时任务 | XXL-Job 每 10 分钟扫描（第二期，第一期可手动）           |
| →已取消        | 管理端   | 校验 `start_time > now` 才可取消                         |

### 2.2 接口清单

**管理端（admin-api：AdminActivityController，路径 /api/admin/community/activity）**

| 方法   | 路径          | 说明                       |
| ------ | ------------- | -------------------------- |
| POST   | /             | 创建活动（草稿或直接发布） |
| PUT    | /             | 修改（仅草稿/招募中）      |
| DELETE | /{id}         | 逻辑删除（仅草稿）         |
| POST   | /{id}/publish | 发布                       |
| POST   | /{id}/cancel  | 取消                       |
| GET    | /page         | 分页列表（状态筛选）       |
| GET    | /{id}         | 详情（含报名列表）         |

**业主端（owner-api：CommunityActivityController，路径 /api/owner/community/activity）**

| 方法   | 路径         | 说明                              |
| ------ | ------------ | --------------------------------- |
| GET    | /page        | 可见活动列表（status IN 1,2,3,4） |
| GET    | /{id}        | 详情（含我是否已报名）            |
| POST   | /{id}/signup | 报名                              |
| DELETE | /{id}/signup | 取消报名（活动开始前）            |
| GET    | /mine        | 我的活动                          |

### 2.3 报名并发控制（核心逻辑）

```java
@Transactional(rollbackFor = Exception.class)
public void signup(Long activityId, Long ownerId, SignupRequest req) {
    // ① 校验活动状态与报名时间窗口
    CommunityActivityEntity activity = activityMapper.selectById(activityId);
    Assert.notNull(activity, "活动不存在");
    Assert.isTrue(ActivityStatusEnum.RECRUITING.matches(activity.getStatus()), "当前不可报名");

    // ② 唯一索引兜底防重复报名
    Long exists = signupMapper.selectCount(new LambdaQueryWrapper<ActivitySignupEntity>()
            .eq(ActivitySignupEntity::getActivityId, activityId)
            .eq(ActivitySignupEntity::getOwnerId, ownerId)
            .ne(ActivitySignupEntity::getStatus, SignupStatusEnum.CANCELED.getValue()));
    if (exists > 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "已报名，勿重复提交");

    // ③ 插入报名记录
    ActivitySignupEntity signup = new ActivitySignupEntity();
    // ... set 字段
    signupMapper.insert(signup);

    // ④ 乐观锁更新冗余计数（version 控制并发满员）
    // UPDATE t_community_activity SET signup_count = signup_count + 1, version = version + 1
    // WHERE id = ? AND version = ?
    int rows = activityMapper.incrSignupCount(activityId, activity.getVersion());
    if (rows == 0) throw new BusinessException(ErrorCode.CONFLICT, "当前报名人数较多，请重试");

    // ⑤ 满员自动流转
    if (activity.getMaxParticipants() > 0
            && activity.getSignupCount() + 1 >= activity.getMaxParticipants()) {
        activityMapper.updateStatus(activityId, ActivityStatusEnum.FULL.getValue());
    }
}
```

**设计说明**：

- 冲突概率低（报名非抢购），用**乐观锁**而非分布式锁 —— 与车位模块同策略
- 唯一索引 `(activity_id, owner_id, status)` 是最终兜底，已取消(status=2)后可重新报名不冲突
- 取消报名：`UPDATE signup SET status=2` + `signup_count - 1`，同一事务

**Mapper 层需补充的自定义方法**：

```java
// CommunityActivityMapper.java
@Update("UPDATE t_community_activity SET signup_count = signup_count + 1, version = version + 1 WHERE id = #{activityId} AND version = #{version}")
int incrSignupCount(@Param("activityId") Long activityId, @Param("version") Integer version);

@Update("UPDATE t_community_activity SET status = #{status} WHERE id = #{activityId}")
int updateStatus(@Param("activityId") Long activityId, @Param("status") Integer status);

// 取消报名时扣减
@Update("UPDATE t_community_activity SET signup_count = signup_count - 1, version = version + 1 WHERE id = #{activityId} AND version = #{version} AND signup_count > 0")
int decrSignupCount(@Param("activityId") Long activityId, @Param("version") Integer version);
```

**Entity 层注意事项**：`CommunityActivityEntity` 需添加 `@Version` 注解的 `version` 字段（参照现有的车位模块乐观锁模式）。

---

## 三、邻里论坛

### 3.1 发帖流程（先发后审）

```
业主提交发帖
   │
   ▼
SensitiveWordFilter.filter(title + content)
   ├─ 干净 ──→ status=1(已发布) 直接可见
   └─ 命中 ──→ status=0(待审核) + 命中词写入 t_forum_post.sensitive_words 字段 → 管理端审核队列
                                        ├─ 通过 → status=1
                                        └─ 驳回 → status=2 + reason
```

管理端另有：置顶（`is_pinned`）、加精（`is_essence`）、删除（`status=3`）。

### 3.2 敏感词过滤（放 framework，供 community 复用）

```java
// property-framework/util/SensitiveWordFilter.java
@Component
public class SensitiveWordFilter {

    private final DFANode root = new DFANode();   // DFA 前缀树
    private volatile long lastLoadTime;

    @PostConstruct
    public void init() { reload(); }

    // 词库支持热更新：存 sys_config(key=sensitive.words)，变更后调 reload()
    public void reload() { /* 从 SysConfigService 读取，重建 DFA 树 */ }

    /** @return 命中的敏感词，空集合表示干净 */
    public List<String> filter(String text) {
        // 经典 DFA 双指针扫描，O(n)
    }
}
```

选择 DFA 而非正则/String.contains：词库上千条时逐条 contains 是 O(n\*m)，DFA 一次扫描 O(n)。

### 3.3 接口清单

**管理端（/api/admin/community/forum）**

| 方法   | 路径               | 说明                      |
| ------ | ------------------ | ------------------------- |
| GET    | /post/page         | 帖子列表（状态/分类筛选） |
| POST   | /post/{id}/audit   | 审核（通过/驳回+原因）    |
| POST   | /post/{id}/pin     | 置顶/取消                 |
| POST   | /post/{id}/essence | 加精/取消                 |
| DELETE | /post/{id}         | 删除                      |
| GET    | /comment/page      | 评论列表                  |
| DELETE | /comment/{id}      | 删除评论                  |

**业主端（/api/owner/community/forum）**

| 方法   | 路径               | 说明                               |
| ------ | ------------------ | ---------------------------------- |
| GET    | /post/page         | 帖子列表（置顶优先，分页）         |
| GET    | /post/{id}         | 帖子详情（+浏览量+1）              |
| POST   | /post              | 发帖（敏感词过滤）                 |
| DELETE | /post/{id}         | 删自己的帖                         |
| GET    | /post/{id}/comment | 评论列表（两级组装）               |
| POST   | /post/{id}/comment | 发评论                             |
| POST   | /like              | 点赞/取消（targetType + targetId） |
| GET    | /mine              | 我的发帖                           |

### 3.4 评论两级组装（查询设计）

```java
// 一次查出该帖全部评论（单帖评论量有限，无需分页；如需分页仅对一级评论分页）
List<ForumCommentEntity> all = commentMapper.selectList(
        new LambdaQueryWrapper<ForumCommentEntity>()
                .eq(ForumCommentEntity::getPostId, postId)
                .eq(ForumCommentEntity::getStatus, CommentStatusEnum.NORMAL.getValue())
                .orderByAsc(ForumCommentEntity::getCreateTime));

// parent_id=0 → 一级；其余挂到对应一级下
Map<Long, List<CommentVO>> childrenMap = all.stream()
        .filter(c -> c.getParentId() != 0)
        .collect(Collectors.groupingBy(ForumCommentEntity::getParentId));

List<CommentVO> result = all.stream()
        .filter(c -> c.getParentId() == 0)
        .peek(vo -> vo.setReplies(childrenMap.getOrDefault(vo.getId(), List.of())))
        .collect(Collectors.toList());
```

发评论时同步 `comment_count + 1`（帖子冗余计数，同事务）。

### 3.5 点赞实现

```java
@Transactional(rollbackFor = Exception.class)
public void like(Long ownerId, Integer targetType, Long targetId) {
    ForumLikeEntity exists = likeMapper.selectOne(...);   // uk 命中
    if (exists == null) {
        likeMapper.insert(new ForumLikeEntity(...));
        countMapper.incrLikeCount(targetType, targetId, 1);   // 帖子/评论 like_count+1
    } else {
        likeMapper.deleteById(exists.getId());                // 取消赞：物理删除
        countMapper.incrLikeCount(targetType, targetId, -1);
    }
}
```

接口幂等：再调一次即取消（toggle 语义），前端点赞按钮防抖。

---

## 四、投票问卷（第二期）

### 4.1 一人一票控制

```sql
-- 投票事务内先插入记录，依赖唯一索引防并发重复投票
INSERT INTO t_vote_record(vote_id, option_id, owner_id) VALUES (?, ?, ?);
-- 若 DuplicateKeyException → 返回"已投过票"
-- 成功后再 UPDATE t_vote_option SET vote_count = vote_count + 1 WHERE id = ?
```

匿名性：`is_anonymous=1` 时结果接口只返回各选项计数，不返回投票人；实名时管理端可查明细。

### 4.2 接口清单

| 端     | 接口                               | 说明                    |
| ------ | ---------------------------------- | ----------------------- |
| 管理端 | POST /api/admin/community/vote     | 创建投票（含选项）      |
| 管理端 | GET /page / {id}                   | 列表/详情（含实时票数） |
| 业主端 | GET /api/owner/community/vote/page | 进行中投票              |
| 业主端 | POST /{id}/cast                    | 投票                    |
| 业主端 | GET /{id}/result                   | 结果（匿名受控）        |

---

## 五、工作量对照

| 任务                                    | 后端   | 备注                   |
| --------------------------------------- | ------ | ---------------------- |
| 模块骨架 + 注册到父 pom/入口/MapperScan | 0.5 天 | 含踩坑清单核查         |
| 活动 CRUD + 状态机（管理端）            | 0.5 天 |                        |
| 活动报名/取消/签到 + 乐观锁             | 0.5 天 |                        |
| 活动业主端列表/详情                     | 0.5 天 |                        |
| 帖子发布/敏感词/审核                    | 0.5 天 | 含 SensitiveWordFilter |
| 评论两级 + 点赞 toggle                  | 1 天   | 组装逻辑 + 冗余计数    |
| 投票（二期）                            | 1 天   |                        |
