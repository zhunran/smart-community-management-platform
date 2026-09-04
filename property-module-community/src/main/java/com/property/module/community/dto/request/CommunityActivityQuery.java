package com.property.module.community.dto.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 社区活动分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommunityActivityQuery extends PageQuery {

    @Schema(description = "活动标题（模糊查询）")
    private String title;

    @Schema(description = "活动类型：1-节日 2-亲子 3-运动 4-讲座 5-其他")
    private Integer activityType;

    @Schema(description = "活动状态：0-草稿 1-招募中 2-已满员 3-进行中 4-已结束 5-已取消")
    private Integer status;

    @Schema(description = "活动地点（模糊查询）")
    private String location;

    @Schema(description = "活动开始时间-起始（范围查询）")
    private LocalDateTime startTimeBegin;

    @Schema(description = "活动开始时间-结束（范围查询）")
    private LocalDateTime startTimeEnd;

    @Schema(description = "活动结束时间-起始（范围查询）")
    private LocalDateTime endTimeBegin;

    @Schema(description = "活动结束时间-结束（范围查询）")
    private LocalDateTime endTimeEnd;
}