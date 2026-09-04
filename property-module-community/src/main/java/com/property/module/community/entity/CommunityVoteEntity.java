package com.property.module.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区投票实体（对应 t_community_vote 表）
 */
@Data
@TableName("t_community_vote")
public class CommunityVoteEntity {

    @TableId
    private Long id;

    private String title;

    private String description;

    /** 1单选 2多选 */
    private Integer voteType;

    /** 1匿名 0实名 */
    private Integer isAnonymous;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 0未开始 1进行中 2已结束 */
    private Integer status;

    @TableLogic
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
