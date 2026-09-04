package com.property.module.bill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增费用标准请求
 */
@Data
public class FeeStandardCreateRequest {

    @Schema(description = "房屋ID（为空表示全局默认标准）")
    private Long roomId;

    @NotNull(message = "费用项ID不能为空")
    @Schema(description = "费用项ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long feeItemId;

    @NotNull(message = "单价不能为空")
    @Schema(description = "单价（元）", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal unitPrice;

    @Schema(description = "生效开始日期（为空立即生效）")
    private LocalDate startDate;

    @Schema(description = "生效结束日期（为空表示长期有效，设置后到期自动失效，适用于季节性调价）")
    private LocalDate endDate;

    @Schema(description = "备注", example = "夏季空调附加费")
    private String remark;
}
