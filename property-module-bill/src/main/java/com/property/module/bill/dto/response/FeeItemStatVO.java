package com.property.module.bill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 费用项维度统计 VO
 */
@Data
@Schema(description = "费用项维度统计")
public class FeeItemStatVO {

    @Schema(description = "费用项ID")
    private Long feeItemId;

    @Schema(description = "费用项名称", example = "物业费")
    private String feeItemName;

    @Schema(description = "应收金额")
    private BigDecimal receivable;

    @Schema(description = "实收金额")
    private BigDecimal received;

    @Schema(description = "应收户数")
    private int billCount;

    @Schema(description = "已缴户数")
    private int paidCount;

    @Schema(description = "收缴率（百分比，如 85.50）")
    private BigDecimal collectionRate;
}
