package com.property.module.bill.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体（对应 t_payment 表）
 */
@Data
@TableName("t_payment")
public class PaymentOrderEntity {

    @TableId
    private Long id;

    private String paymentNo;

    private Long billId;

    private Long roomId;

    private Long ownerId;

    private Integer paymentMethod;

    private BigDecimal paymentAmount;

    private LocalDateTime paymentTime;

    private String transactionId;

    private Integer paymentStatus;

    private BigDecimal refundAmount;

    private String payerName;

    private String payerPhone;

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
