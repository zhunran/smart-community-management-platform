package com.property.module.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投票选项实体（对应 t_vote_option 表）
 */
@Data
@TableName("t_vote_option")
public class VoteOptionEntity {

    @TableId
    private Long id;

    private Long voteId;

    private String content;

    private Integer voteCount;

    private Integer sortOrder;

    @TableLogic
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
