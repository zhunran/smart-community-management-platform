package com.property.module.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付下单请求
 */
@Data
@Schema(description = "支付下单请求")
public class PayOrderRequest {

    @NotNull(message = "账单ID不能为空")
    @Schema(description = "账单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long billId;

    @NotNull(message = "支付方式不能为空")
    @Schema(description = "支付方式：1-支付宝(ALIPAY) 4-现金(CASH) 5-转账(TRANSFER)", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer paymentMethod;

    @Schema(description = "付款人姓名")
    private String payerName;

    @Schema(description = "备注")
    private String remark;
}
