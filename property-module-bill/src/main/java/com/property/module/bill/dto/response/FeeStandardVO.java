package com.property.module.bill.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用标准 VO
 */
@Data
public class FeeStandardVO {

    @Schema(description = "标准ID")
    private Long id;

    @Schema(description = "房屋ID")
    private Long roomId;

    @Schema(description = "房屋编号")
    private String roomCode;

    @Schema(description = "费用项ID")
    private Long feeItemId;

    @Schema(description = "费用项名称")
    private String feeItemName;

    @Schema(description = "单价（元）")
    private BigDecimal unitPrice;

    @Schema(description = "生效开始日期")
    private LocalDate startDate;

    @Schema(description = "生效结束日期")
    private LocalDate endDate;

    @Schema(description = "状态：0-停用 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
