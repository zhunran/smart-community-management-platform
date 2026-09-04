1. 计费与账单:

- "统一计价模型怎么抽象的？如果新增一种费用类型怎么做？"
- "账单怎么保证幂等？重复跑生成任务会怎样？"
- "为什么账单要拆明细？整单和分项缴费有什么区别？"
- 回答要点: calcType 1-4 (面积×单价×月数 / 单价×月数 / 预估用量×单价 / 固定金额)，单价优先级（费用标准覆盖 > 费用项默认），月数映射（月1季3半年6年12）；幂等 = 同房屋同账期存在非终态账单则跳过；账单号 BILL+yyyyMMdd+4位流水；状态机 UNPAID→PARTIAL→PAID + OVERDUE，终态不可再缴；整单用 FOR UPDATE 行级锁，分项用 SET paid_amount=paid_amount+? 原子累加；BillItemAllocator 按明细顺序分摊。
- 深挖: 为什么要分摊到明细（对账、部分缴费、退款精度）；罚息上限100%；为什么金额场景用悲观锁。

2. 在线收缴:

- "为什么三重防重还不够？每层分别解决什么盲区？"
- "回调丢失怎么办？"
- "下单防重怎么做的？"
- 回答要点: 下单防重（同账单已有待支付订单复用）；回调流程验签 RSA2 → 只处理 TRADE_SUCCESS/FINISHED → Redis 锁(30s) → FOR UPDATE → 幂等检查(终态跳过) → 状态机 canTransitionTo → 乐观锁 WHERE status=? → 更新账单 paidAmount+重算状态+分摊 → finally 释放锁；主动对账（扫描30分钟前待确认订单，queryTradeSafe 安全查询，超时≥30分钟标记FAILED，<30分钟跳过，WAIT_BUYER_PAY跳过，TRADE_SUCCESS乐观锁更新）；@Scheduled 每2分钟 + XXL-Job 每2小时双保险。
- 强调: 分布式锁防并发、行级锁串行化、乐观锁最终兜底、幂等防重复——"至少一次投递、恰好一次生效"。

3. 逾期催缴:

- "罚息怎么算？"
- "怎么防止重复发邮件？"
- "催缴任务依赖什么配置？"
- 回答要点: 扫描 due_date<today 且 status IN(0,1,5)；罚息=未缴×日利率×逾期天数（上限100%），日利率默认0.001从 sys_config 读（late.fee.rate/late.fee.days 热配置）；按 ownerId 分组查邮箱，Thymeleaf 模板 overdue-notice；XXL-Job 每日09:00；@ConditionalOnProperty 邮件未配置不影响启动。
- 深挖: sys_config 热配置的意义（不改代码调费率）。

4. AI 客服:

- "Function Calling 怎么接入真实数据的？"
- "为什么用 SSE 不用 WebSocket？"
- "会话上下文怎么隔离？"
- 回答要点: Spring AI 2.0.0 + OpenAI 兼容协议接 DeepSeek；ChatClient.stream() 流式；MessageChatMemoryAdvisor + ChatMemory 按 sessionId 隔离；系统提示词从 sys_config 读（ai.chat.system_prompt，限制150字引导调用工具）；4个@Tool（queryMyBills/queryMyRooms/queryLatestNotices/queryCommunityBrief），工具内用 SecurityUtil.getUserId() 取当前业主，所以数据天然按业主隔离；SSE 单向流式够用、更简单、易穿透代理、自动携带 httpOnly Cookie；前端 fetch+ReadableStream 打字机。
- 深挖: 为什么工具函数防止"编造"；会话标题取首条消息前20字。
- 回答样例: "防编造靠 Function Calling：把查账单、查房屋、查公告、查小区简报包装成 4 个 @Tool，AI 需要数据时调工具查真实结果再作答，而不是靠模型记忆编。每个工具里用 SecurityUtil.getUserId() 取当前登录业主，数据天然按人隔离。多轮记忆用 MessageChatMemoryAdvisor + ChatMemory 按 sessionId 隔离。为什么用 SSE：回复是单向流式，SSE 够用还更简单，能自动带 httpOnly Cookie 鉴权，前端 fetch+ReadableStream 做打字机效果。"

5. 社区互动:

- "怎么防止一人多投/重复报名？"
- "敏感词过滤怎么做的？"
- "状态机怎么设计的？"
- 回答要点: 活动状态机 草稿→招募中→满员/进行中→结束/取消，资格内聚枚举(canSignup等)；报名 incrSignupCount 原子递增+查重+满员自动流转；投票 单选/多选、匿名/实名、状态 未开始/进行中/已结束 仅进行中可投、同业主同投票查重、incrVoteCount 原子累加、结果按选项聚合 ownerIds；论坛 发帖敏感词命中转待审核、PostStatusEnum 待审核/已发布/已驳回/已删除、两级评论树(parentId)、点赞 toggle 去重、置顶精华软删除。
- 深挖: 为什么社区用原子递增而不用锁（冲突后果轻、频率低）。

6. 便民服务:

- "场地预约怎么检测冲突的？"
- "报修状态机怎么流转？并发怎么控制？"
- "通行码怎么保证安全唯一？"
- 回答要点: 报修 8态 待审核→待派单→已派单→维修中→已完成→已评价 + 已驳回/已取消；工单号 RP+yyyyMMdd+4位流水；审核/派单/接单/完工 selectByIdForUpdate 行级锁；场地类型1-5；冲突检测 = 同场地同日期 endTime>start 且 startTime<end 存在有效预约(isActive)即拒绝；月上限；开始前2小时可取消超时违约；通行码 6位 SecureRandom + 生成后查重，状态 有效/已用尽/已过期/已撤销，核销递减次数。
- 回答样例: "报修 8 个状态：待审核→待派单→已派单→维修中→已完成→已评价，另有已驳回/已取消；工单号 RP+yyyyMMdd+4位流水；审核、派单、接单、完工都用 selectByIdForUpdate 行级锁串行化，防止重复派单。场地预约冲突用区间重叠判断：同场地同日期，新预约 endTime>已存在 start 且 新 startTime<已存在 end 就重叠拒绝，再加月上限和开始前 2 小时可取消。访客通行码用 6 位 SecureRandom + 生成后查重保证唯一，因为涉及门禁安全，不用普通随机数。"

General prep advice:

- 每个点都能"一句话讲业务痛点→方案→技术点→为什么这样设计→量化效果"
- 准备画图：架构分层、支付回调时序、报修状态机
- 准备被问"你负责哪部分""最大难点""踩过什么坑"（可以提中文乱码 utf8mb4/PowerShell 编码、Long→String 精度、并发 401 只刷一次）
- 用 STAR 结构
- 诚实：数据/效果不要编造

I should keep it organized and not overly verbose. But given it's an interview prep, thoroughness is valuable. I'll aim for a solid, scannable answer.

I should not use file links excessively here since it's a general answer, but I can reference the docs I just wrote. Actually, linking to the summary docs could help them review. I could mention they can deep-dive via the summary docs. Maybe one link at the end.

Let me write in Chinese. Use headers and bullets. Keep each item's "回答要点" tight but concrete.

I'll structure:

## 通用准备思路 (short)

Then per item: **追问方向** + **回答要点/关键词**
Then 结尾: 高频必问 + 常见坑

Actually to be efficient, I'll do per item with "可能被追问" and "怎么答（要点）" in a compact way.

Let me write it.
下面按你简历的六条，逐条给出**面试官可能的追问方向 + 你该怎么答的要点**。所有内容都来自你项目真实实现，回答时可以直接用，别编造数字和效果。

---

## 通用回答结构（每条都套这个）

> **痛点一句话 → 我怎么设计 → 关键技术点 → 为什么这么选（权衡）→ 结果/收益**

面试官追问通常三类：**“怎么实现的”**（要细节）、**“为什么这么做”**（要设计权衡）、**“踩过什么坑”**（要反思）。下面每条都覆盖。

---

## 1. 计费与账单

**可能被追问：**

- “统一计价模型具体怎么抽象的？新增一种费用类型要改代码吗？”
- “账单怎么保证幂等？定时任务重复跑会怎样？”
- “整单缴费和分项缴费有什么区别？并发怎么处理？”

**回答要点：**

- **计价抽象**：`calcType` 四种公式——按面积（面积×单价×月数）、按户（单价×月数）、按用量（预估用量×单价）、固定金额（单价）。新增费用类型只配参数（费用项+费用标准），不改代码。
- **单价优先级**：费用标准覆盖 > 费用项默认单价，匹配条件是 `roomId:feeItemId` 且标准有效期覆盖账单日期。
- **幂等**：同房屋同账期已存在非终态账单就跳过；账单号 `BILL+yyyyMMdd+4位流水`。所以任务重跑不会重复收费。
- **状态机**：`未缴 → 部分缴 → 已缴清`，外加逾期状态；终态（已缴清/已作废/已减免）不可再缴。
- **并发分级（这是亮点）**：整单缴费用 `SELECT ... FOR UPDATE` 行级锁，分项缴费用 `SET paid_amount = paid_amount + ?` 原子累加。原因是金额场景冲突后果严重，宁可串行。

---

## 2. 在线收缴

**可能被追问：**

- “你说三重防重，具体是哪三层？各解决什么盲区？”
- “回调丢了/重复推送怎么办？”
- “下单防重怎么做的？”

**回答要点（这是最容易拿分的，讲透）：**

- 严格说是**四层**：`Redis 分布式锁（SET NX EX，30s）→ 行级锁 FOR UPDATE → 乐观锁 WHERE status=? → 幂等检查（终态跳过）`。简历写“三重防重”是把前三层概括了，幂等检查是最终兜底——被追问时可以主动补这句，显得理解到位。
- **每层作用**：分布式锁防并发重复处理；行级锁串行化；乐观锁最终兜底（affectedRows=0 即失败）；幂等检查防已到终态被重复处理。
- **回调流程**：验签 RSA2 → 只处理 `TRADE_SUCCESS/FINISHED` → 四层防重 → 状态机校验 → 更新账单已缴金额+重算状态+分摊到明细。
- **主动对账补偿**：不依赖被动回调，定时扫 30 分钟前待确认订单，逐条安全查询支付宝交易状态纠正——即使回调丢了，账单也会被补对（“最终一致”）。
- **下单防重**：同账单已有待支付订单则复用，不重复建单。
- **双保险**：管理端 `@Scheduled` 每 2 分钟 + XXL-Job 每 2 小时两套对账。

---

## 3. 逾期催缴

**可能被追问：**

- “罚息怎么算？利率写死还是可配？”
- “怎么避免重复发邮件？”

**回答要点：**

- 扫描条件：`due_date < 今天` 且状态在 `未缴/部分缴/逾期`。
- **罚息公式**：未缴金额 × 日利率 × 逾期天数，**上限 100%**；日利率默认 0.001，从 `sys_config` 表热配置读取（`late.fee.rate`/`late.fee.days`）——改费率不用改代码、不用重启。
- **按户分组**：按 ownerId 分组 → 查业主邮箱 → 用 Thymeleaf 模板批量发催缴邮件。
- 邮件服务用 `@ConditionalOnProperty` 条件注册，没配邮件也不影响系统启动（这个细节能体现工程化意识）。

---

## 4. AI 客服

**可能被追问：**

- “AI 怎么查到真实业务数据？不怕它编造吗？”
- “为什么用 SSE 不用 WebSocket？”
- “多轮会话上下文怎么隔离的？”

**回答要点：**

- **技术栈**：Spring AI 2.0.0 + OpenAI 兼容协议接 DeepSeek 大模型。
- **防编造的关键是 Function Calling**：把业务查询包装成 4 个 `@Tool`（查账单、查房屋、查最新公告、小区简报），AI 需要时调用工具查真实数据再作答，而不是凭模型记忆编。
- **数据隔离**：工具函数内用 `SecurityUtil.getUserId()` 取当前登录业主，天然按业主过滤，不会越权查到别人账单。
- **SSE vs WebSocket**：AI 回复是单向流式，SSE 足够且更简单、易穿透代理、能自动携带 httpOnly Cookie 鉴权。前端用原生 `fetch + ReadableStream` 做打字机效果。
- **会话记忆**：`MessageChatMemoryAdvisor + ChatMemory` 按 sessionId 隔离多轮上下文。

---

## 5. 社区互动

**可能被追问：**

- “怎么防止一人多投 / 重复报名？”
- “敏感词过滤怎么做的？”
- “这么多状态，状态机怎么设计的？”

**回答要点：**

- **状态机 + 资格内聚到枚举**：活动 `草稿→招募中→满员/进行中→结束/取消`，投票 `未开始/进行中/已结束`；“能不能报名/投票/审核”这些规则写在枚举方法里（`canSignup()/canVote()/canAudit()`），Controller 只调用不散落判断。
- **防重复**：报名/投票都是“同业主+同活动/投票”查重，已存在即拒绝。
- **计数用原子递增不用锁**：`incrSignupCount`/`incrVoteCount` 做 `SET count = count + 1`，报名达上限自动转“满员”。**为什么不用锁**——社区场景冲突后果轻、频率低，原子递增开销小（这条能和报修的悲观锁形成对比，体现“按风险分级选并发策略”）。
- **内容合规**：发帖敏感词命中自动转“待审核”，人工审核通过才发布；帖子状态 `待审核/已发布/已驳回/已删除`。
- **论坛其他**：两级评论树（父评论+子回复）、点赞 toggle 去重、置顶/精华/软删除。

**回答样例：**「防一人多投和重复报名，都是先按同业主+同投票/活动查重，存在就拒绝；计数用 SET count=count+1 原子递增，报名数到上限自动把活动流转成满员。状态规则我内聚到枚举，canSignup/canVote 这些写在枚举方法上，Controller 只调用不散落判断。论坛发帖做敏感词过滤，命中自动转待审核、人工通过才发布；评论两级树、点赞 toggle 去重、置顶参与排序、精华做展示标记。社区不用锁是因为冲突后果轻、频率低，原子递增开销更小，这跟报修的悲观锁形成对比，体现按风险分级选并发策略。」

---

## 6. 便民服务

**可能被追问：**

- “场地预约冲突怎么检测的？”
- “报修状态机怎么流转？并发怎么控制？”
- “访客通行码怎么保证安全唯一？”

**回答要点：**

- **报修 8 态**：`待审核→待派单→已派单→维修中→已完成→已评价`，另有“已驳回/已取消”；工单号 `RP+yyyyMMdd+4位流水`；审核/派单/接单/完工都用 `selectByIdForUpdate` 行级锁串行化，防止重复派单、丢单。
- **场地冲突检测**：同场地同日期，判断时间区间是否相交——`新预约 endTime > 已存在 start 且 新 startTime < 已存在 end` 就拒绝；再加“同业主同场地月上限”和“开始前 2 小时可取消，超时违约”。用**区间重叠判断**而非加锁，比锁更精确。
- **通行码**：6 位 `SecureRandom` 安全随机数 + 生成后查重保证唯一；状态 `有效/已用尽/已过期/已撤销`，核销递减可用次数。涉及门禁安全所以用 `SecureRandom` 而不是普通随机。

**回答样例：**「报修 8 个状态：待审核→待派单→已派单→维修中→已完成→已评价，另有已驳回/已取消；工单号 RP+yyyyMMdd+4 位流水；审核、派单、接单、完工都用 selectByIdForUpdate 行级锁串行化，防止重复派单、丢单。场地预约冲突用区间重叠判断：同场地同日期，新预约 endTime 大于已存在 start 且新 startTime 小于已存在 end 就重叠拒绝，再加月上限和开始前 2 小时可取消。访客通行码用 6 位 SecureRandom 加生成后查重保证唯一，因为涉及门禁安全，不用普通随机数。」

---

## 最后：准备时再补这几点

1. **准备画图**：整体分层架构、支付回调时序图、报修/活动状态机图——被要求“讲讲整体设计”时直接画。
2. **准备一个“最难/踩坑”的故事**（STAR）：你项目里现成的坑——数据库中文乱码（utf8mb4 但 PowerShell 管道编码导致 `?`）、Long 精度丢失（前端 `startTime` undefined 那类）、并发 401 只刷一次。挑一个最熟的讲透。
3. **被问“你负责哪部分”**：选 2-3 个你最有把握的模块认领（建议：在线收缴 + 计费账单，技术密度最高）。
4. **数据别编**：被问“效果如何/量级多大”，没有真实数字就答“演示项目，数据是造的数据，重点是验证方案正确性”——比编一个假 QPS 更专业。

需要的话，我可以把这份内容整理成一份「追问自测清单」或按某个模块展开成更细的 Q&A。
