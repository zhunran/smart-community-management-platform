package com.property.module.parking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车位信息实体（对应 t_parking_space 表）
 */
@Data
@TableName("t_parking_space")
public class ParkingSpaceEntity {

    @TableId
    private Long id;

    private String spaceCode;

    private String spaceName;

    private Integer spaceType;

    private BigDecimal area;

    private String floor;

    private String zone;

    @Version
    private Integer version;

    private Long ownerId;

    private Long roomId;

    private Integer rentalType;

    private BigDecimal monthlyFee;

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
