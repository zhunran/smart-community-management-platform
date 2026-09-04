package com.property.module.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投票记录实体（对应 t_vote_record 表）
 */
@Data
@TableName("t_vote_record")
public class VoteRecordEntity {

    @TableId
    private Long id;

    private Long voteId;

    private Long optionId;

    private Long ownerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
