package com.property.module.owner.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭成员实体（对应 t_owner_family 表）
 */
@Data
@TableName("t_owner_family")
public class OwnerFamilyEntity {

    @TableId
    private Long id;

    private Long ownerId;

    private String familyName;

    private String relationship;

    private String phone;

    private String idCardNo;

    private Integer gender;

    private LocalDate birthday;

    private Integer isEmergency;

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
