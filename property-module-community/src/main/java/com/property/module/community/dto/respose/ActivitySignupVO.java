package com.property.module.community.dto.respose;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名记录 VO
 */
@Data
public class ActivitySignupVO {

    @Schema(description = "报名记录ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "活动ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long activityId;

    @Schema(description = "活动标题")
    private String title;

    @Schema(description = "活动开始时间")
    private LocalDateTime activityStartTime;

    @Schema(description = "活动结束时间")
    private LocalDateTime activityEndTime;

    @Schema(description = "活动地点")
    private String location;

    @Schema(description = "参与人数")
    private Integer participants;

    @Schema(description = "报名状态：0-已报名 1-已签到 2-已取消")
    private Integer status;

    @Schema(description = "报名状态名称")
    private String statusName;

    @Schema(description = "报名时间")
    private LocalDateTime signupTime;

    @Schema(description = "签到时间")
    private LocalDateTime checkinTime;
}