package com.property.module.parking.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车位变更日志实体（对应 t_parking_change_log 表）
 */
@Data
@TableName("t_parking_change_log")
public class ParkingChangeLogEntity {

    @TableId
    private Long id;

    private Long spaceId;

    private String spaceCode;

    private String changeType;

    private Long oldOwnerId;

    private Long newOwnerId;

    private Long oldRoomId;

    private Long newRoomId;

    private Integer oldStatus;

    private Integer newStatus;

    private String remark;

    private String operator;

    @TableLogic
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
