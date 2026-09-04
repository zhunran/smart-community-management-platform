# 物业管理系统面试问答集（八股 + 项目技术点）

> 技术栈：Spring Boot + MyBatis-Plus + MySQL + Redis + Spring WebFlux(SSE) + Vue3/Vant
> 说明：本文档结合项目**实际代码**整理，面试时先讲八股原理，再落到项目里的真实用法和踩坑点。

---

## 第一部分：经典八股问题集（对应项目实际）

### 一、Java 基础

#### Q1：HashMap 底层原理？项目中哪里用到了 HashMap？
**八股要点**
- JDK1.7：数组 + 链表；JDK1.8：数组 + 链表 + 红黑树（链表>=8且数组>=64 转红黑树，退化为链表 < 6）
- 默认容量 16，负载因子 0.75，扩容阈值为 `容量 × 负载因子 = 12`
- 扩容：`resize()` 翻倍（2 的幂），元素重哈希定位到原位置或 `原位置+旧容量`
- 为什么容量是 2 的幂：`(n-1) & hash` 取模，位运算效率高且分布均匀
- `hash` 方法：`h ^ (h >>> 16)` 高 16 位参与扰动，减少碰撞

**项目落地**
- 常见于关联映射缓存，如 [BillServiceImpl.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-bill/src/main/java/com/property/module/bill/service/impl/BillServiceImpl.java#L593-L594) 用 `HashMap<Long, Long>` / `HashMap<Long, String>` 缓存 `buildingId -> buildingName` 关联，避免多次查库。支付、物业模块同样用 Map 做 `billMap/ownerInfoMap` 关联。

#### Q2：ArrayList 和 LinkedList 区别？项目中用的哪个？
**八股要点**
- 底层结构：`Object[]` vs 双向链表
- 随机访问：ArrayList O(1) 下标，LinkedList O(n) 需遍历
- 插入/删除：ArrayList 尾插 O(1)、中间 O(n) 因搬移；LinkedList 头尾 O(1)、中间 O(n) 因定位
- 内存：ArrayList 连续内存节省空间；LinkedList 每个节点额外存前后指针
- 线程安全：都不同步，需 `Collections.synchronizedList` / `CopyOnWriteArrayList`

**项目落地**
- 全项目只用 `ArrayList`（如报表导出缓存、服务层结果组装 [DashboardServiceImpl.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-bill/src/main/java/com/property/module/bill/service/impl/DashboardServiceImpl.java#L76)），因为业务以"顺序追加 + 遍历"为主，正好契合 ArrayList 语义。

#### Q3：== 与 equals 区别？项目中如何比较？
**八股要点**
- `==`：比较基本类型值 / 引用类型地址
- `equals`：Object 的默认实现等价 `==`，`String` 等重写后比较内容
- `equals` 重写必须同时重写 `hashCode`（违反则 HashMap/HashSet 行为异常）

**项目落地**
- 实体类未手写 `equals/hashCode`（由 MyBatis-Plus + Lombok 兜底），业务中大量用 `String.equals` / 枚举 `==` 比较值，如状态判断 `targetStatus != PaymentStatusEnum.SUCCESS`（枚举用 `==` 是安全的）。

---

### 二、并发

#### Q1：ThreadLocal 原理？为什么陈久？项目中怎么用？
**八股要点（本考点被项目直接命中）**
- `ThreadLocal` 是线程本地变量：每个线程维护自己的 `ThreadLocalMap`
- 数据结构：`ThreadLocalMap` 的 key 是**弱引用 `ThreadLocal`**（`Entry extends WeakReference<ThreadLocal>`），value 是强引用
- **内存泄漏根源**：key 弱引用 GC 后变 null，但 value 仍是强引用，若线程长期存活（如 Tomcat 线程池复用），`Entry.value` 永远无法回收
- **解决方案**：用完必须调用 `remove()`；get/set/remove 时会触发 `expungeStaleEntry` 清理 key 为 null 的过期 Entry

**项目落地（最佳回答范本）**
- [SecurityUtil.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/SecurityUtil.java#L12) 用 `ThreadLocal<LoginUser> USER_HOLDER` 存当前登录用户
- `setLoginUser/getUserId/clear` 三件套；关键在 [AuthInterceptor.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/web/security/AuthInterceptor.java#L86) 的 `afterCompletion` **请求结束调用 `clear()` 即 `USER_HOLDER.remove()`**，就是为了防内存泄漏。

#### Q2：为什么项目没直接用线程池/synchronized？并发控制靠什么？
**八股要点**
- synchronized 锁升级：无锁 -> 偏向锁 -> 轻量级锁 -> 重量级锁
- volatile 保证可见性 + 禁止指令重排，但不保证原子性
- ThreadPoolExecutor 七大参数：corePoolSize、maximumPoolSize、keepAliveTime、unit、workQueue、threadFactory、拒绝策略

**项目落地**
- 项目**无** ThreadPoolExecutor/synchronized/volatile（AI 模块有被注释的线程池代码，未启用）
- 并发安全依赖两层：
  1. **MySQL 数据库层面**：`FOR UPDATE` 行锁 + 乐观锁 `WHERE status=?`（见支付防重）
  2. **Redis 层面**：`SET NX EX` 分布式锁（见第二部分）
- 定时任务用 `@Scheduled`（如 [ReconciliationTask.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/java/com/property/adminapi/task/ReconciliationTask.java) 每 120s 对账），由 Spring 内置线程池驱动。

---

### 三、JVM

#### Q1：OOM 如何排查？
**八股要点**
1. 加 `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/path` 自动导出 dump
2. 用 MAT / jvisualvm 分析 `heapdump.hprof`，看"大对象、实例数最高"的类
3. 结合 GC 日志 `-Xlog:gc` 看是否 `Full GC 频繁`、老年代持续不回收
4. 常见场景：ThreadLocal 泄漏（见并发 Q1）、集合无限增长、连接未关闭

**项目落地**
- [Dockerfile](file:///d:/.workspace/javaproject/property-management-system/property-management/docker/Dockerfile) 已配置 `-Xms256m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/`，OOM 时自动生成堆转储，配合运行 JDK 21（eclipse-temurin）排查。

---

### 四、MySQL

#### Q1：@Transactional 用在哪些场景？为什么加 rollbackFor？
**八股要点**
- 默认只回滚 `RuntimeException` 和 `Error`，**不会**回滚受检异常（`Exception` 且非 RunTime）
- 加 `rollbackFor = Exception.class` 可让所有异常都触发回滚
- 传播机制：REQUIRED（默认，有则用，无则建）、REQUIRES_NEW（挂起旧的建新的）、NOT_SUPPORTED、NEVER、SUPPORTS 等
- `isolation` 隔离级别：READ_UNCOMMITTED / READ_COMMITTED / REPEATABLE_READ / SERIALIZABLE

**项目落地**
- 大量 Service 使用 `@Transactional(rollbackFor = Exception.class)`，如账单、业主、停车、支付对账等模块
- 编程式事务：Excel 导入/导出用 `TransactionTemplate` + `TransactionDefinition.PROPAGATION_REQUIRES_NEW`，实现逐批提交避免长事务（[OwnerExcelService.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-admin-api/src/main/java/com/property/adminapi/service/excel/OwnerExcelService.java)）
- 项目默认用 MySQL 的 REPEATABLE_READ 隔离级别（默认），未显式配置。

#### Q2：乐观锁 / 悲观锁分别怎么实现的？项目里怎么搭配？
**八股要点**
- 悲观锁：`SELECT ... FOR UPDATE`，读时即加锁，其他事务阻塞。适合写多读少/竞争激烈
- 乐观锁：加 version/status 字段，`UPDATE ... WHERE status=期望值 (version=旧值)`，影响行数=0 则冲突。适合读多写少
- 二阶段锁 / MVCC 实现可见性

**项目落地（核心亮点）**
- [PaymentCallbackTxService.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-payment/src/main/java/com/property/module/payment/service/PaymentCallbackTxService.java#L52) 支付回调**三层防重**：
  1. `@Transactional` 包一层保证原子性
  2. [BillPaymentMapper.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-bill/src/main/java/com/property/module/bill/repository/BillPaymentMapper.java#L56) `selectByPaymentNoForUpdate` 用 **`SELECT ... FOR UPDATE` 行级锁**锁住该支付单
  3. [BillPaymentMapper.java#L66](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-bill/src/main/java/com/property/module/bill/repository/BillPaymentMapper.java#L66) `updatePaymentStatus` 用 **乐观锁 `WHERE payment_no=? AND payment_status=期望原状态`**，返回 0 说明已被并发改过，直接判 failure 二次防重

#### Q3：MyBatis 动态条件查询 / 分页怎么做？
**项目落地**
- 用 MyBatis-Plus `LambdaQueryWrapper` 做类型安全动态条件
- 分页用 `LambdaQueryWrapper.last("LIMIT n")`（如 [BillServiceImpl.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-bill/src/main/java/com/property/module/bill/service/impl/BillServiceImpl.java#L277)）
- 统计查重用 `COUNT`（如 [OwnerMapper.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-owner/src/main/java/com/property/module/owner/repository/OwnerMapper.java)）
- 无 Mapper XML，全部注解式 `@Select/@Insert/@Update`，复杂联表用 `@Select` 直接写 SQL（如 [ParkingWarningMapper.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-parking/src/main/java/com/property/module/parking/repository/ParkingWarningMapper.java)）

---

### 五、Redis

#### Q1：分布式锁怎么实现？为什么用 Lua 脚本释放？
**八股要点（本考点被项目直接命中）**
- 加锁用 `SET key value NX EX 过期时间`（原子）：`NX` 保证不存在才设置，`EX` 设置过期防死锁，`NX EX` 一次性完成避免竞态
- 释放锁用 **Lua 脚本**保证"判断持有者 + 删除"原子性，否则可能误删别人刚获取的锁（过期后 key 被复用时）
- 生产高可靠可用 Redisson（支持看门狗自动续期、可重入）；`SET NX EX` 缺点是锁过期导致并发执行

**项目落地（最佳回答范本）**
- [RedisUtil.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/util/RedisUtil.java#L112) `tryLock(lockKey, requestId, timeout)` 内部调用 `opsForValue().setIfAbsent(key, value, timeout)`（即 SET NX EX），`requestId` 用 UUID 标识持有者
- `unlock(lockKey, requestId)` [RedisUtil.java#L128](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/util/RedisUtil.java#L128) 用 Lua：`if get(key)==requestId then del(key) else return 0`，保证只有持有者能删
- 锁 Key 规范：`lock:payment:callback:PAY2026...`

#### Q2：缓存穿击/失效/雪崩？项目里缓存怎么用？
**八股要点**
- 穿透：查不存在数据频繁打库 -> 布隆过滤器 or 缓存空值
- 击穿：单个热点 key 过期，大量请求打库 -> 互斥锁/逻辑过期
- 雪崩：大量 key 同时过期 -> 过期时间加随机值、多级缓存

**项目落地**
- [SysConfigService.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/service/SysConfigService.java#L80) `@Cacheable(value="sysConfig", key="#key", unless="#result == null")` 缓存系统配置，[RedisConfig.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/config/RedisConfig.java) 设 TTL 30 分钟
- 配置变更调用 `@CacheEvict` 的 `refreshCache(key)` 主动失效
- 项目**未**做穿透/击穿/雪崩专项处理，面试时讲理论 + 说明后续可加布隆过滤器互斥锁。

#### Q3：Redis 为什么快？数据类型有哪些？
**八股要点**
- 纯内存操作 + 单线程 IO 多路复用（6.0 后多线程用于网络 IO，命令执行仍单线程）
- 数据类型：String / Hash / List / Set / ZSet / Bitmap / HyperLogLog / Geo
- 项目用到的：String（KV、分布式锁、token）、RedisTemplate + Jackson 序列化 value。

---

### 六、Spring

#### Q1：循环依赖怎么解决？Spring 三级缓存？
**八股要点（本考点被项目间接命中）**
- Spring 三级缓存解决**字段注入/构造注入除外**的循环依赖：
  1. `singletonObjects`（一级：成品）
  2. `earlySingletonObjects`（二级：早期暴露的半成品裸 bean）
  3. `singletonFactories`（三级：ObjectFactory 用于提前生成 AOP 代理）
- 流程：A 创建时提前放入三级缓存(工厂)，引用 B 时从 B 暴露的裸 bean 注入，B 引用 A 时从三级取可以，最后 A 完成属性注入并返回
- 解决不了：构造器注入循环、Prototype 作用域

**项目落地（更贴近面试的"自注入"）**
- [SysConfigService.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-framework/src/main/java/com/property/framework/service/SysConfigService.java#L26) 构造器里 `@Lazy SysConfigService self` **自注入代理**，目的是让 `self.getConfig()` 走 Spring AOP 代理，否则同类内部方法直接调用会**绕过 `@Cacheable` 代理失效**。
- 因此更好的解释是：**"自调用绕代理" 问题**（`this` 调用不走代理），解决方式有注入自身代理、注入 ApplicationContext、拆独立 Service。

#### Q2：@Transactional 为什么同类调用会失效？
**八股要点**
- `@Transactional` 靠 AOP 代理生效，`this.xxx()` 直接调用不走代理
- 解决：注入自身代理（`@Lazy self`）、拆独立 Service（把事务逻辑放另一个 Bean）、`TransactionTemplate`

**项目落地**
- [PaymentCallbackTxService.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-payment/src/main/java/com/property/module/payment/service/PaymentCallbackTxService.java#L23) 类注释明确："**独立 Service 确保 @Transactional 生效**"，即把支付回调事务逻辑从 Controller/外部调用方拆到单独 Bean，保证事务始终经代理执行。

#### Q3：IOC / AOP 原理？Bean 生命周期？
**八股要点**
- IOC：控制反转，容器管理对象创建与依赖注入（构造器/字段/方法）
- AOP：JDK 动态代理（接口）与 CGLIB（类），切入点 + 通知织入
- Bean 生命周期：实例化 -> 属性填充 -> Aware 回调 -> BeanPostProcessor#before -> init(InitializingBean/@PostConstruct) -> 使用 -> BeanPostProcessor#after -> destroy
- 项目用构造器注入为主（`@RequiredArgsConstructor + final`），少量 `@Autowired(required=false)`（可空支付服务）

**项目落地**
- `@PostConstruct` 用于支付宝配置 Bean 初始化加载（[AlipayConfig.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-payment/src/main/java/com/property/module/payment/config/AlipayConfig.java#L42)）
- 全局横切用 `@RestControllerAdvice`（`GlobalExceptionHandler` 统一异常、`ResponseAdvice` 统一返回包装）
- 无自定义 `@Aspect`，AOP 以隐式 `@Transactional/@Cacheable` 出现。

---

### 七、网络

#### Q1：HTTP 请求怎么鉴权的？
**项目落地（本考点体现明显）**
- JWT 签发与校验：`JwtUtil` + `TokenService`（Redis 存 token 及失效/黑名单）
- JWT Header/Unix 时间戳 -> Token 刷新
- SSO 单点登录：`AuthInterceptor` 解析 Token 后 `SecurityUtil.setLoginUser` 写入 ThreadLocal，请求结束 `clear()`
- 全局基础校验：`TraceFilter`（`@Order(HIGHEST_PRECEDENCE)`）实现 traceId 追踪

#### Q2：AI 对话为什么用 SSE 而不是 WebSocket？
**八股要点**
- SSE（Server-Sent Events）：单向（服务器->客户端）、基于 HTTP、断线自动重连、天然兼容 Nginx 代理、文本流式友好
- WebSocket：双向、需单独握手升级协议、适合即时双向通信

**项目落地（本考点是项目特色）**
- [ChatController.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/java/com/property/ownerapi/controller/ChatController.java#L68) `@PostMapping(value="/send", produces=MediaType.TEXT_EVENT_STREAM_VALUE)` 返回 `Flux<String>`，后端用 Reactor 流式拼接 AI 回复，前端 `fetch` + `ReadableStream.getReader()` 增量解析 `data:` 行。AI 模块还用到 `Schedulers.boundedElastic()` 把阻塞 JDBC 操作（保存历史）切到独立线程，避免阻塞 SSE 流。

---

### 八、设计模式

#### Q1：项目里用了哪些设计模式？（结合项目讲更有说服力）
**八股要点**
- 单例：创建 5 种写法（饿汉、懒汉双重校验、静态内部类、枚举、静态代码块）
- 代理：静态/动态代理，CGLIB 与 JDK 区别
- 工厂/策略/模板

**项目落地（本考点最能体现深度）**
1. **代理模式**：Spring AOP 代理让 `@Transactional/@Cacheable` 生效，以及"自注入代理"绕 CGLIB 限制（SysConfigService.self）
2. **状态机模式（重点讲）**：[PaymentStatusEnum.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-module-payment/src/main/java/com/property/module/payment/enums/PaymentStatusEnum.java) 定义支付状态流转
   - `isFinal()` 终态（SUCCESS/REFUNDED/PARTIAL_REFUND）不可再转
   - `canTransitionTo(target)` 校验合法迁移（WAITING/PROCESSING -> SUCCESS/FAILED；SUCCESS -> 退款）
   - `fromAlipayTradeStatus()` 完成支付宝状态到本地状态映射
   - 配合 `@Transactional` 回调处理非终态幂等跳过，避免重复扣款
3. **模板/回调模式**：Excel 导入监听器（EasyExcel `AnalysisEventListener`）+ `TransactionTemplate` 编程式事务
4. **单例**：由 Spring 容器默认单例隐式实现。

---

## 第二部分：项目重要技术点深挖（自定义问答）

### 1. 整个 AI 对话功能的完整链路是怎样的？
- **前端** [ChatView.vue](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-web/src/views/chat/ChatView.vue) 用 `fetch` + 流式读取接收 SSE，边收边把 chunk 追加进消息气泡
- **后端** [ChatController.java](file:///d:/.workspace/javaproject/property-management-system/property-management/property-owner-api/src/main/java/com/property/ownerapi/controller/ChatController.java#L68) `produces=TEXT_EVENT_STREAM_VALUE` 返回 `Flux<String>`，前端一行一个 `data:` 推送
- **会话管理**：请求无 sessionId 时后端自动 `createSession` 造会话；`ChatMemory.CONVERSATION_ID` 按 sessionId 隔离上下文，实现**多会话记忆互不干扰**
- **标题自动生成**：取首条用户消息前 20 字做会话标题
- **历史**：按 sessionId 查询 t_chat_history
- 前端专门处理了流式结束标记、打字气泡（首包到达后消失）、过滤双引号等体验问题。

### 2. 支付回调为什么做了三层防重？各自解决什么问题？
- **`@Transactional`**：保证整段数据库操作原子性，任一步失败整体回滚，不产生"状态已改但账单没改"的脏数据
- **`SELECT ... FOR UPDATE` 行级锁**：并发的**多条支付宝重试通知**同时进来时，串行执行，避免同时读到 WAITING
- **乐观锁 `WHERE payment_status=期望原状态`**：即使行锁未能 100% 兜底（或超时释放），更新时若发现状态已被改，影响行数=0，返回 failure，**二次幂等防重**
- 三者叠加 = 数据库隔离级别之上的业务级原子 + 幂等，保证"一笔订单资金只被确收一次"。

### 3. 状态机在支付中怎么防呆、防错？
- `isFinal()`：SUCCESS/REFUNDED 等终态直接 return success，支付宝重复通知直接幂等跳过，**不重复加钱**
- `canTransitionTo()`：只允许合法迁移，非法状态（如从 FAILED 直接转 SUCCESS）直接拒绝，**避免脏状态回写**
- 状态由枚举集中管理，代码里不散落魔法数字 0/1/2/3。

### 4. ThreadLocal 存登录用户，为什么不直接用传参？
- 避免在每个 Service 方法签名里都传 userId，业务代码直接 `SecurityUtil.getUserId()` 即可取当前用户
- 一次请求内天然线程隔离（一个请求原则上由一个线程处理）
- 隐患：**线程池复用线程**导致 ThreadLocal 残留，所以必须 `afterCompletion` 里 `clear()`（remove），这正是 ThreadLocal 内存泄漏考题的落点。

### 5. Redis 分布式锁为什么配 requestId + Lua？
- requestId（UUID）标识持有者，防止"锁已过期被 B 获取，A 再来释放时把 B 的锁删掉"
- Lua 把"compare + delete"做成**单个原子脚本**，避免 get 后线程切换导致误删
- key 带业务前缀 + 业务号（`lock:payment:callback:PAY...`），配合 `EX` 过期防死锁。

### 6. 为什么用 MyBatis-Plus 注解 SQL 而不用 XML？
- 简单 CRUD 直接继承 BaseMapper，动态查询用 LambdaQueryWrapper（类型安全，编译期防写错字段名）
- 复杂 SQL（FOR UPDATE、乐观锁更新、联表统计）用 `@Select/@Update/@Insert` 注解内联，配置集中、无需 XxxMapper.xml 文件
- 分页用 `.last("LIMIT n")` 快速物理分页，天然对接 MySQL。

### 7. AI 模块里 `ClassCastException / Reactor 阻塞问题` 是怎么踩坑和解决的？
- 现象：在 SSE 的 `doOnComplete` 回调里直接执行同步 JDBC 保存历史，导致 Reactor 响应式流与阻塞 IO 冲突
- 修复：把保存历史的阻塞操作放到 `Mono.fromRunnable(...).subscribeOn(Schedulers.boundedElastic()).subscribe()`，用弹性线程池承载阻塞 JDBC，不阻塞 SSE 推送线程
- 这是 WebFlux + 传统 JDBC 混用时的典型取舍，也解释了为何本项目 AI 模块用 Reactor 但支付等模块用普通 Spring MVC。

---

## 附：一段可直接背的"项目亮点自我总结"

> 本项目是基于 Spring Boot + MyBatis-Plus + MySQL + Redis 的物业管理系统，主要亮点：
> 1. **支付回调幂等防重**：`@Transactional` + `SELECT ... FOR UPDATE` 行锁 + `WHERE 原状态` 乐观锁三层叠加，配合状态机枚举 `canTransitionTo`，保证一笔订单资金只被确收一次；
> 2. **Redis 分布式锁**：`SET NX EX` 加锁 + Lua 脚本原子释放 + requestId 持有者标识，用于回调等并发敏感场景；
> 3. **登录上下文**：ThreadLocal 承载 JWT 用户 + 请求结束 `remove()` 防泄漏；
> 4. **AI 多会话对话**：Spring WebFlux 流式 SSE 输出 + 按 sessionId 隔离上下文 + ChatMemory 会话记忆；
> 5. **缓存**：Spring Cache + Redis 缓存系统配置，`@Cacheable/@CacheEvict` 主动失效；
> 6. **异常统一**：`@RestControllerAdvice` 全局异常 + 统一响应包装。