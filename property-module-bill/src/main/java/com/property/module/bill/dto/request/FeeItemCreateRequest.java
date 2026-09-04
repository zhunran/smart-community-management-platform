package com.property.module.bill.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增费用项请求 DTO
 */
@Data
public class FeeItemCreateRequest {

    @Schema(description = "费用项编码", example = "PROPERTY_FEE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "费用项编码不能为空")
    private String itemCode;

    @Schema(description = "费用项名称", example = "物业费", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "费用项名称不能为空")
    private String itemName;

    @Schema(description = "计费周期：1-月 2-季 3-半年 4-年 5-一次性", example = "1")
    private Integer billingCycle;

    @Schema(description = "计费方式：1-面积 2-户 3-用量 4-固定金额", example = "1")
    private Integer calcType;

    @Schema(description = "单价（元）", example = "2.50")
    private BigDecimal unitPrice;

    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态：0-停用 1-启用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
