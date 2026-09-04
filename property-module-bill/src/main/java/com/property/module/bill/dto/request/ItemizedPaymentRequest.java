package com.property.module.bill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 分项缴费请求
 */
@Data
@Schema(description = "分项缴费请求")
public class ItemizedPaymentRequest {

    @NotNull(message = "账单ID不能为空")
    @Schema(description = "账单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long billId;

    @NotEmpty(message = "缴费明细不能为空")
    @Valid
    @Schema(description = "缴费明细列表（指定要缴费的费用项及金额）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ItemPayment> items;

    @NotNull(message = "支付方式不能为空")
    @Schema(description = "支付方式：4-现金(CASH) 5-转账(TRANSFER)", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Integer paymentMethod;

    @Schema(description = "付款人姓名", example = "张三")
    private String payerName;

    @Schema(description = "备注", example = "前台缴纳物业费")
    private String remark;

    @Data
    @Schema(description = "单笔费用项缴费明细")
    public static class ItemPayment {

        @NotNull(message = "账单明细ID不能为空")
        @Schema(description = "账单明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long billItemId;

        @NotNull(message = "缴费金额不能为空")
        @Schema(description = "本次缴费金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
        private BigDecimal amount;
    }
}
