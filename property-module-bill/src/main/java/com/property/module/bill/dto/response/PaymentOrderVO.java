package com.property.module.bill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录 VO
 */
@Data
@Schema(description = "支付记录")
public class PaymentOrderVO {

    @Schema(description = "支付ID")
    private Long id;

    @Schema(description = "支付单号")
    private String paymentNo;

    @Schema(description = "关联账单ID")
    private Long billId;

    @Schema(description = "房屋ID")
    private Long roomId;

    @Schema(description = "业主ID")
    private Long ownerId;

    @Schema(description = "支付方式：1-支付宝 2-微信 3-银行卡 4-现金 5-转账 6-其他")
    private Integer paymentMethod;

    @Schema(description = "支付方式名称")
    private String paymentMethodName;

    @Schema(description = "支付金额")
    private BigDecimal paymentAmount;

    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;

    @Schema(description = "第三方支付流水号")
    private String transactionId;

    @Schema(description = "支付状态：0-待支付 1-支付中 2-支付成功 3-支付失败 4-已退款 5-部分退款")
    private Integer paymentStatus;

    @Schema(description = "支付状态名称")
    private String paymentStatusName;

    @Schema(description = "已退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "付款人姓名")
    private String payerName;

    @Schema(description = "付款人手机号")
    private String payerPhone;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // ===== 关联信息 =====

    @Schema(description = "楼栋名称")
    private String buildingName;

    @Schema(description = "房号")
    private String roomCode;

    @Schema(description = "房屋名称")
    private String roomName;

    @Schema(description = "业主姓名")
    private String ownerName;

    @Schema(description = "业主手机号")
    private String ownerPhone;

    @Schema(description = "账期")
    private String billPeriod;

    @Schema(description = "账单编号")
    private String billNo;
}
