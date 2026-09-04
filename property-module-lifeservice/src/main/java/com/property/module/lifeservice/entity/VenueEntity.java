package com.property.module.lifeservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场地实体（对应 t_venue 表）
 */
@Data
@TableName("t_venue")
public class VenueEntity {

    @TableId
    private Long id;

    private String name;

    /** 1健身房 2棋牌室 3会议室 4游泳池 5其他 */
    private Integer venueType;

    private String location;

    private Integer capacity;

    private LocalTime openTime;

    private LocalTime closeTime;

    /** 预约粒度（分钟） */
    private Integer slotMinutes;

    /** 每业主每月上限（0=不限） */
    private Integer monthlyLimit;

    /** 费用（0=免费） */
    private BigDecimal price;

    /** 0停用 1启用 */
    private Integer status;

    @TableLogic
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
