package com.property.module.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付下单结果
 */
@Data
@Schema(description = "支付下单结果")
public class PayOrderResult {

    @Schema(description = "支付记录ID")
    private Long paymentId;

    @Schema(description = "支付单号")
    private String paymentNo;

    @Schema(description = "支付方式：1-支付宝 4-现金 5-转账")
    private Integer paymentMethod;

    @Schema(description = "支付方式名称")
    private String paymentMethodName;

    @Schema(description = "是否在线支付（需前端跳转支付网关）")
    private Boolean onlinePay;

    @Schema(description = "支付表单HTML（在线支付时返回，前端直接渲染即可跳转）")
    private String payFormHtml;
}
