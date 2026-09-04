package com.property.module.statistic.vo;

import lombok.Data;

@Data
public class FeeTrendPointVO {
    private String date;          // 日期 yyyy-MM-dd
    private Long payerCount;      // 当日缴费人数（去重业主）
    private java.math.BigDecimal amount; // 当日缴费金额
}

