package com.property.module.housing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 楼栋实体（对应 t_building 表）
 */
@Data
@TableName("t_building")
public class BuildingEntity {

    @TableId
    private Long id;

    private String buildingCode;

    private String buildingName;

    private Integer totalUnits;

    private Integer totalFloors;

    private Integer totalRooms;

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