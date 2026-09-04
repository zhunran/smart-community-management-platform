package com.property.module.statistic.constant;

import java.util.Set;

/**
 * 审计风险动作定义。
 * 与各 Controller 已埋点的 {@code @OperationLog(action = "...")} 文案对齐。
 * 命中规则：action 以"删除"开头，或在精确清单中。
 */
public final class RiskActionConstant {

    private RiskActionConstant() {
    }

    /** 精确命中的财务/导入/对账类风险动作 */
    private static final Set<String> RISK_ACTIONS = Set.of(
            "整单缴费",
            "分项缴费",
            "手动对账",
            "批量导入缴费记录",
            "执行双轨对账"
    );

    /** 删除类前缀 */
    private static final String DELETE_PREFIX = "删除";

    public static boolean isRisk(String action) {
        if (action == null) {
            return false;
        }
        return action.startsWith(DELETE_PREFIX) || RISK_ACTIONS.contains(action);
    }
}