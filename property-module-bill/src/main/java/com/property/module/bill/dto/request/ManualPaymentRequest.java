package com.property.module.bill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员手动标记缴费请求
 */
@Data
@Schema(description = "手动缴费请求")
public class ManualPaymentRequest {

    @NotNull(message = "账单ID不能为空")
    @Schema(description = "账单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long billId;

    @NotNull(message = "支付方式不能为空")
    @Schema(description = "支付方式：4-现金(CASH) 5-转账(TRANSFER)", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Integer paymentMethod;

    @Schema(description = "付款人姓名（为空默认取业主姓名）", example = "张三")
    private String payerName;

    @Schema(description = "备注", example = "前台现金缴纳")
    private String remark;
}
