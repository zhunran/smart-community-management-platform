package com.property.module.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账单主表实体（对应 t_bill 表）
 */
@Data
@TableName("t_bill")
public class BillEntity {

    @TableId
    private Long id;

    private String billNo;

    private Long roomId;

    private Long ownerId;

    private String billPeriod;

    private Integer billType;

    private LocalDate billDate;

    private LocalDate dueDate;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private BigDecimal discountAmount;

    private BigDecimal lateFee;

    private Integer status;

    private String remark;

    @TableLogic
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
