package com.property.module.statistic.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditSummaryVO {
    private Long totalCount;          // 统计区间内总操作数
    private Long failCount;           // 失败数
    private BigDecimal failRate;      // 失败率（failCount/totalCount，四舍五入保留2位）
    private List<AuditModuleStatVO> moduleTop;   // 按模块 TopN
    private List<AuditUserStatVO>   userTop;     // 按操作者 TopN
    private Map<String, Long> riskActionCount;   // 风险动作次数（按 action 计数）

}
