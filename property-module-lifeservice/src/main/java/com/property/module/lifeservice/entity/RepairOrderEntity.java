package com.property.module.lifeservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报修工单实体（对应 t_repair_order 表）
 */
@Data
@TableName("t_repair_order")
public class RepairOrderEntity {

    @TableId
    private Long id;

    private String orderNo;

    private Long ownerId;

    private Long roomId;

    private Integer category;

    private String title;

    private String description;

    private String images;

    private Integer urgency;

    private Long handlerId;

    private String handleNote;

    private String rejectReason;

    private Integer rating;

    private String ratingComment;

    private Integer status;

    private Integer timeoutFlag;

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