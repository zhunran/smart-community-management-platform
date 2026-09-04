package com.property.module.bill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 修改费用标准请求
 */
@Data
public class FeeStandardUpdateRequest {

    @NotNull(message = "标准ID不能为空")
    @Schema(description = "标准ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "单价（元）")
    private BigDecimal unitPrice;

    @Schema(description = "生效开始日期")
    private LocalDate startDate;

    @Schema(description = "生效结束日期（为空表示长期有效）")
    private LocalDate endDate;

    @Schema(description = "状态：0-停用 1-启用", example = "1")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
