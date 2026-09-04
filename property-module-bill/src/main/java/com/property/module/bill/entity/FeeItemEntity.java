package com.property.module.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用项实体（对应 t_fee_item 表）
 */
@Data
@TableName("t_fee_item")
public class FeeItemEntity {

    @TableId
    private Long id;

    private String itemCode;

    private String itemName;

    private Integer billingCycle;

    private Integer calcType;

    private BigDecimal unitPrice;

    private Integer sortOrder;

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
