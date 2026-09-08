package com.property.module.ai.router;

/**
 * AI 对话意图枚举
 *
 * <p>用于意图路由的双路径分流：</p>
 * <ul>
 *   <li>{@link #BUSINESS}——业务数据查询（账单/房屋/公告/车位等），走主链路查库（CommunityInfoTool）</li>
 *   <li>{@link #REGULATION}——规章/流程/办理类咨询，优先走 DocQuery RAG（可选增强路径）</li>
 *   <li>{@link #OTHER}——其余/寒暄/分类失败，走主链路兜底对话</li>
 * </ul>
 */
public enum AiIntent {

    BUSINESS,
    REGULATION,
    OTHER
}