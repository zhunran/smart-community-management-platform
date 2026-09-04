package com.property.module.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增社区活动请求 DTO
 */
@Data
public class CommunityActivityCreateRequest {

    @Schema(description = "活动标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "活动标题不能为空")
    @Size(max = 200, message = "活动标题长度不能超过200")
    private String title;

    @Schema(description = "活动内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "活动内容不能为空")
    private String content;

    @Schema(description = "封面图片URL")
    @Size(max = 500, message = "封面图片URL长度不能超过500")
    private String coverImage;

    @Schema(description = "活动类型：1-节日 2-亲子 3-运动 4-讲座 5-其他", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "活动类型不能为空")
    private Integer activityType;

    @Schema(description = "活动地点", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "活动地点不能为空")
    @Size(max = 200, message = "活动地点长度不能超过200")
    private String location;

    @Schema(description = "组织者", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "组织者不能为空")
    @Size(max = 100, message = "组织者长度不能超过100")
    private String organizer;

    @Schema(description = "活动开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "活动结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "报名开始时间")
    private LocalDateTime signupStart;

    @Schema(description = "报名截止时间")
    private LocalDateTime signupEnd;

    @Schema(description = "最大参与人数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "最大参与人数不能为空")
    private Integer maxParticipants;

    @Schema(description = "活动状态：0-草稿 1-发布（招募中），默认草稿")
    private Integer status;
}