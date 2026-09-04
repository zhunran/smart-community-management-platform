package com.property.module.parking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车位使用记录实体（对应 t_parking_usage 表）
 */
@Data
@TableName("t_parking_usage")
public class ParkingUsageEntity {

    @TableId
    private Long id;

    private Long spaceId;

    private Long ownerId;

    private Long vehicleId;

    private String plateNo;

    private Integer usageType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal durationHours;

    private BigDecimal feeAmount;

    private Integer paymentStatus;

    private Long paymentId;

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
