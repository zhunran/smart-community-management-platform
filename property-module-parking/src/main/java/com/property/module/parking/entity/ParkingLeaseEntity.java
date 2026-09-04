package com.property.module.parking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车位租赁合同实体（对应 t_parking_lease 表）
 */
@Data
@TableName("t_parking_lease")
public class ParkingLeaseEntity {

    @TableId
    private Long id;

    private String contractNo;

    private Long spaceId;

    private Long ownerId;

    private String plateNo;

    private LocalDate leaseStart;

    private LocalDate leaseEnd;

    private BigDecimal monthlyFee;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private Integer paymentMethod;

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
