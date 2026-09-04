package com.property.module.bill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 首页仪表盘统计 VO
 */
@Data
@Schema(description = "首页仪表盘统计")
public class DashboardVO {

    @Schema(description = "当月应收金额")
    private BigDecimal currentMonthReceivable;

    @Schema(description = "当月实收金额")
    private BigDecimal currentMonthReceived;

    @Schema(description = "累计欠费金额")
    private BigDecimal totalArrears;

    @Schema(description = "当月收缴率（百分比，如 85.50）")
    private BigDecimal collectionRate;

    @Schema(description = "统计月份", example = "2026-06")
    private String statisticsMonth;
}
