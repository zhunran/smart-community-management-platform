package com.property.module.owner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 业主-房屋关联实体（对应 t_owner_room 表）
 */
@Data
@TableName("t_owner_room")
public class OwnerRoomEntity {

    @TableId
    private Long id;

    private Long ownerId;

    private Long roomId;

    private Integer relationType;

    private Integer isPrimary;

    private LocalDate moveInTime;

    private LocalDate moveOutTime;

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
