package com.property.module.bill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用项响应 VO
 */
@Data
public class FeeItemVO {

    @Schema(description = "费用项ID")
    private Long id;

    @Schema(description = "费用项编码")
    private String itemCode;

    @Schema(description = "费用项名称")
    private String itemName;

    @Schema(description = "计费周期：1-月 2-季 3-半年 4-年 5-一次性")
    private Integer billingCycle;

    @Schema(description = "计费方式：1-面积 2-户 3-用量 4-固定金额")
    private Integer calcType;

    @Schema(description = "单价（元）")
    private BigDecimal unitPrice;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：0-停用 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
