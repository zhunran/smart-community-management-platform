package com.property.module.owner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业主车辆实体（对应 t_owner_vehicle 表）
 */
@Data
@TableName("t_owner_vehicle")
public class OwnerVehicleEntity {

    @TableId
    private Long id;

    private Long ownerId;

    private String plateNo;

    private Integer vehicleType;

    private String vehicleColor;

    private String vehicleBrand;

    private Long parkingSpaceId;

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
