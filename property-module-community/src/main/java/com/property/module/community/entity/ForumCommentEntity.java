package com.property.module.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛评论实体（对应 t_forum_comment 表）
 */
@Data
@TableName("t_forum_comment")
public class ForumCommentEntity {

    @TableId
    private Long id;

    private Long postId;

    private Long parentId;

    private Long replyTo;

    private Long ownerId;

    private String content;

    private Integer likeCount;

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