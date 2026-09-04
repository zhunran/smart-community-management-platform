package com.property.module.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单明细实体（对应 t_bill_item 表）
 * 与 BillEntity 为 1:N 关系
 */
@Data
@TableName("t_bill_item")
public class BillItemEntity {

    @TableId
    private Long id;

    private Long billId;

    private Long feeItemId;

    private String feeItemName;

    private BigDecimal calcBase;

    private BigDecimal unitPrice;

    private BigDecimal quantity;

    private BigDecimal amount;

    private BigDecimal discountAmount;

    private BigDecimal paidAmount;

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
