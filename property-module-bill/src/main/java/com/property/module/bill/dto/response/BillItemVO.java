package com.property.module.bill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账单明细 VO
 */
@Data
public class BillItemVO {

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "费用项ID")
    private Long feeItemId;

    @Schema(description = "费用项名称")
    private String feeItemName;

    @Schema(description = "计费基数（面积/用量/户数）")
    private BigDecimal calcBase;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "数量（月数/次数）")
    private BigDecimal quantity;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "减免金额")
    private BigDecimal discountAmount;

    @Schema(description = "已交金额")
    private BigDecimal paidAmount;

    @Schema(description = "备注")
    private String remark;
}
