package com.property.module.housing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房屋实体（对应 t_room 表）
 */
@Data
@TableName("t_room")
public class RoomEntity {

    @TableId
    private Long id;

    private Long buildingId;

    private Long unitId;

    private String roomCode;

    private String roomName;

    private Integer floor;

    private Integer roomType;

    private BigDecimal area;

    private BigDecimal usableArea;

    private String orientation;

    private Integer decorationStatus;

    private Integer occupancyStatus;

    private BigDecimal propertyFeeRate;

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