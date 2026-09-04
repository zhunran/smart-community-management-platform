package com.property.module.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用标准实体（对应 t_fee_standard 表）
 * 每个房屋每种费用的单价覆盖，可覆盖费用项默认单价
 */
@Data
@TableName("t_fee_standard")
public class FeeStandardEntity {

    @TableId
    private Long id;

    private Long roomId;

    private Long feeItemId;

    private BigDecimal unitPrice;

    private LocalDate startDate;

    private LocalDate endDate;

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
