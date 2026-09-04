package com.property.module.housing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单元实体（对应 t_unit 表）
 */
@Data
@TableName("t_unit")
public class UnitEntity {

    @TableId
    private Long id;

    private Long buildingId;

    private String unitCode;

    private String unitName;

    private Integer totalFloors;

    private Integer totalRooms;

    private Integer elevatorCount;

    private Integer sortOrder;

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