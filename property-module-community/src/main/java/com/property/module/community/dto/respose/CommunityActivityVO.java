package com.property.module.community.dto.respose;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区活动列表 VO
 */
@Data
public class CommunityActivityVO {

    @Schema(description = "活动ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "活动标题")
    private String title;

    @Schema(description = "封面图片URL")
    private String coverImage;

    @Schema(description = "活动类型：1-节日 2-亲子 3-运动 4-讲座 5-其他")
    private Integer activityType;

    @Schema(description = "活动类型名称")
    private String activityTypeName;

    @Schema(description = "活动地点")
    private String location;

    @Schema(description = "组织者")
    private String organizer;

    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @Schema(description = "报名开始时间")
    private LocalDateTime signupStart;

    @Schema(description = "报名截止时间")
    private LocalDateTime signupEnd;

    @Schema(description = "最大参与人数")
    private Integer maxParticipants;

    @Schema(description = "当前报名人数")
    private Integer signupCount;

    @Schema(description = "活动状态：0-草稿 1-招募中 2-已满员 3-进行中 4-已结束 5-已取消")
    private Integer status;

    @Schema(description = "活动状态名称")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}