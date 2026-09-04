package com.property.module.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名记录实体（对应 t_activity_signup 表）
 */
@Data
@TableName("t_activity_signup")
public class ActivitySignupEntity {

    @TableId
    private Long id;

    private Long activityId;

    private Long ownerId;

    private Integer participants;

    private Integer status;

    private LocalDateTime signupTime;

    private LocalDateTime checkinTime;

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