package com.property.module.bill.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单详情 VO（含明细列表）
 */
@Data
public class BillDetailVO {

    @Schema(description = "账单ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "账单编号")
    private String billNo;

    @Schema(description = "房屋ID")
    private Long roomId;

    @Schema(description = "业主ID")
    private Long ownerId;

    @Schema(description = "账期")
    private String billPeriod;

    @Schema(description = "账单类型：1-周期性 2-临时 3-滞纳金")
    private Integer billType;

    @Schema(description = "出账日期")
    private LocalDate billDate;

    @Schema(description = "缴费截止日期")
    private LocalDate dueDate;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "已交金额")
    private BigDecimal paidAmount;

    @Schema(description = "减免金额")
    private BigDecimal discountAmount;

    @Schema(description = "滞纳金")
    private BigDecimal lateFee;

    @Schema(description = "状态：0-未缴费 1-部分缴费 2-已缴清 3-已作废 4-已减免")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // ===== 关联信息 =====

    @Schema(description = "房号")
    private String roomCode;

    @Schema(description = "房屋名称")
    private String roomName;

    @Schema(description = "业主姓名")
    private String ownerName;

    @Schema(description = "业主手机号")
    private String ownerPhone;

    // ===== 明细列表 =====

    @Schema(description = "账单明细列表")
    private List<BillItemVO> items;
}
