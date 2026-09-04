package com.property.module.owner.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 业主信息实体（对应 t_owner 表）
 */
@Data
@TableName("t_owner")
public class OwnerEntity {

    @TableId
    private Long id;

    private String ownerName;

    private String phone;

    @JsonIgnore
    private String password;

    private Integer idCardType;

    private String idCardNo;

    private Integer gender;

    private LocalDate birthday;

    private String email;

    private String emergencyContact;

    private String emergencyPhone;

    private String avatar;

    private Integer ownerType;

    private Integer status;

    private LocalDateTime registerTime;

    private LocalDateTime lastLoginTime;

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
