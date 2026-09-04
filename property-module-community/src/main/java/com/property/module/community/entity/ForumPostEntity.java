package com.property.module.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛帖子实体（对应 t_forum_post 表）
 */
@Data
@TableName("t_forum_post")
public class ForumPostEntity {

    @TableId
    private Long id;

    private String title;

    private String content;

    private String images;

    private Integer category;

    private Long ownerId;

    private Long roomId;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer isPinned;

    private Integer isEssence;

    private String rejectReason;

    private String sensitiveWords;

    private Integer status;

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