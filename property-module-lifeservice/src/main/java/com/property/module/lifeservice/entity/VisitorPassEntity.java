package com.property.module.lifeservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客通行码实体（对应 t_visitor_pass 表）
 */
@Data
@TableName("t_visitor_pass")
public class VisitorPassEntity {

    @TableId
    private Long id;

    private String passCode;

    private Long ownerId;

    private String visitorName;

    private String visitorPhone;

    private String plateNo;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    /** 可用次数（0=不限） */
    private Integer maxUse;

    private Integer usedCount;

    /** 0有效 1已用尽 2已过期 3已撤销 */
    private Integer status;

    @TableLogic
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
