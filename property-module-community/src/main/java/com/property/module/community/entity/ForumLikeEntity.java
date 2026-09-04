package com.property.module.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞记录实体（对应 t_forum_like 表）
 */
@Data
@TableName("t_forum_like")
public class ForumLikeEntity {

    @TableId
    private Long id;

    private Long targetId;

    private Integer targetType;

    private Long ownerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}