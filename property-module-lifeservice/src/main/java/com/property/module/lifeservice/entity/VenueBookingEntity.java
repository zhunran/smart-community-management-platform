package com.property.module.lifeservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场地预约实体（对应 t_venue_booking 表）
 */
@Data
@TableName("t_venue_booking")
public class VenueBookingEntity {

    @TableId
    private Long id;

    private Long venueId;

    private Long ownerId;

    private LocalDate bookingDate;

    private LocalTime startTime;

    private LocalTime endTime;

    /** 0已预约 1已使用 2已取消 3已违约 */
    private Integer status;

    @TableLogic
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
