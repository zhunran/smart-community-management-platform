package com.property.module.ai.router;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * 意图路由（关键词粗分流）
 *
 * <p>采用纯字符串匹配，零 LLM 开销、零失败路径：任何输入都能返回一个合法意图，
 * 绝不向主链路传播异常。业务词优先，其次规章/流程词，最后归 OTHER。</p>
 *
 * <p><b>设计取舍：</b>未采用 LLM 分类 prompt，一是避免每轮额外消耗模型配额与引入额外延迟，
 * 二是保住"分类环节绝不影响主链路"的硬约束（关键词匹配不可能抛异常）。后续如需更高准确率，
 * 可平滑升级为轻量模型分类，并沿用"失败一律归 OTHER"的兜底策略。</p>
 */
@Component
public class AiIntentRouter {

    /** 业务数据查询词：命中即走主链路查库（数据最新且真实，优先于 DocQuery） */
    private static final List<String> BUSINESS_KEYWORDS = List.of(
            "物业费", "账单", "缴费", "水费", "电费", "燃气费", "欠费", "待缴",
            "车位", "房屋", "房号", "我的房", "面积", "公告", "通知");

    /** 规章/流程/办理类咨询词：命中即尝试 DocQuery RAG（可用时） */
    private static final List<String> REGULATION_KEYWORDS = List.of(
            "装修", "报修流程", "怎么报修", "维修", "流程", "规定", "规范", "制度",
            "怎么申请", "需要什么", "材料", "办理", "物业服务中心", "客服电话", "联系方式", "收费标准");

    /**
     * 路由意图。
     *
     * <p>业务词优先，其次规章/流程词，其余（含空消息）归 {@link AiIntent#OTHER}。</p>
     *
     * @param message 用户消息
     * @return 意图枚举，永不为 null
     */
    public AiIntent route(String message) {
        if (message == null || message.isBlank()) {
            return AiIntent.OTHER;
        }
        for (String kw : BUSINESS_KEYWORDS) {
            if (message.contains(kw)) {
                return AiIntent.BUSINESS;
            }
        }
        for (String kw : REGULATION_KEYWORDS) {
            if (message.contains(kw)) {
                return AiIntent.REGULATION;
            }
        }
        return AiIntent.OTHER;
    }
}