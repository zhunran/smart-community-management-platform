package com.property.module.notification.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小区公告实体（对应 t_notice 表）
 */
@Data
@TableName("t_notice")
public class NoticeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 公告类型：NOTICE / WATER_ELECTRIC / ACTIVITY / EMERGENCY */
    private String type;

    /** 状态：0=草稿，1=已发布，2=已下线 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 创建人ID（管理员） */
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}