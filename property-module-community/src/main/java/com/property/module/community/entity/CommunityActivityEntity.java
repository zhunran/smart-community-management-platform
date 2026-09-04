package com.property.module.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 社区活动实体（对应 t_community_activity 表）
 */
@Data
@Getter
@TableName("t_community_activity")
public class CommunityActivityEntity {

    @TableId
    private Long id;

    private String title;

    private String content;

    private String coverImage;

    private Integer activityType;

    private String location;

    private String organizer;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime signupStart;

    private LocalDateTime signupEnd;

    private Integer maxParticipants;

    private Integer signupCount;

    private Integer status;

    @Version
    private Integer version;

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