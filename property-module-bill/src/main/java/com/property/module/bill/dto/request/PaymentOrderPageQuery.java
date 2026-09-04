package com.property.module.bill.dto.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 支付记录分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentOrderPageQuery extends PageQuery {

    @Schema(description = "账单ID")
    private Long billId;

    @Schema(description = "房屋ID")
    private Long roomId;

    @Schema(description = "业主ID")
    private Long ownerId;

    @Schema(description = "支付方式：1-支付宝 2-微信 3-银行卡 4-现金 5-转账 6-其他")
    private Integer paymentMethod;

    @Schema(description = "支付状态：0-待支付 1-支付中 2-支付成功 3-支付失败 4-已退款 5-部分退款")
    private Integer paymentStatus;

    @Schema(description = "支付单号（模糊查询）")
    private String paymentNo;

    @Schema(description = "付款人（模糊查询）")
    private String payerName;

    @Schema(description = "支付时间起始")
    private LocalDateTime paymentTimeStart;

    @Schema(description = "支付时间结束")
    private LocalDateTime paymentTimeEnd;
}
