package com.property.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体（对应 t_sys_user 表）
 */
@Data
@TableName("t_sys_user")
public class SysUserEntity {

    @TableId
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    private String avatar;

    private Integer gender;

    private Long deptId;

    private String post;

    private Integer userType;

    private Integer status;

    private String lastLoginIp;

    private LocalDateTime lastLoginTime;

    private Integer pwdErrorCount;

    private LocalDateTime pwdUpdateTime;

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