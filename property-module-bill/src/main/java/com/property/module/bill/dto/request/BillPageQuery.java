package com.property.module.bill.dto.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账单分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BillPageQuery extends PageQuery {

    @Schema(description = "房屋ID")
    private Long roomId;

    @Schema(description = "业主ID")
    private Long ownerId;

    @Schema(description = "楼栋ID")
    private Long buildingId;

    @Schema(description = "账期（模糊查询）", example = "2024-01")
    private String billPeriod;

    @Schema(description = "账期起始（yyyy-MM）", example = "2024-01")
    private String billPeriodStart;

    @Schema(description = "账期结束（yyyy-MM）", example = "2024-06")
    private String billPeriodEnd;

    @Schema(description = "账单类型：1-周期性 2-临时 3-滞纳金")
    private Integer billType;

    @Schema(description = "状态：0-未缴费 1-部分缴费 2-已缴清 3-已作废 4-已减免")
    private Integer status;

    @Schema(description = "是否含停车费：true-仅含停车费 false-不含停车费 null-不限制")
    private Boolean hasParkingFee;

    @Schema(description = "房号（模糊查询）", example = "A1-1-101")
    private String roomCode;
}
