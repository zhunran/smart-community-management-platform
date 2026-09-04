package com.property.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置实体（对应 t_sys_config 表）
 */
@Data
@TableName("t_sys_config")
public class SysConfigEntity {

    @TableId
    private Long id;

    private String configKey;

    private String configValue;

    private Integer configType;

    private String groupName;

    private String description;

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
