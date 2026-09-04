package com.property.module.owner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投诉建议实体（对应 t_complaint 表）
 */
@Data
@TableName("t_complaint")
public class ComplaintEntity {

    @TableId
    private Long id;

    private Long ownerId;

    private Long roomId;

    private Integer complaintType;

    private String title;

    private String content;

    private String images;

    private Integer urgencyLevel;

    private Integer status;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime handleTime;

    private String handleResult;

    private Integer ownerRating;

    private String ownerFeedback;

    private LocalDateTime closeTime;

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
