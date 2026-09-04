package com.property.module.lifeservice.dto.repair.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报修工单 VO
 */
@Data
public class RepairOrderVO {

    @Schema(description = "工单ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "工单号 RP+yyyyMMdd+4位流水")
    private String orderNo;

    @Schema(description = "报修人ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ownerId;

    @Schema(description = "报修房屋ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long roomId;

    @Schema(description = "报修分类：1-水电 2-门窗 3-电梯 4-公共设施 5-其他")
    private Integer category;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "问题描述(简)")
    private String title;

    @Schema(description = "问题详情")
    private String description;

    @Schema(description = "现场照片URL列表")
    private String images;

    @Schema(description = "紧急程度：1-普通 2-紧急 3-特急")
    private Integer urgency;

    @Schema(description = "紧急程度名称")
    private String urgencyName;

    @Schema(description = "维修员ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long handlerId;

    @Schema(description = "处理说明")
    private String handleNote;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "评价1-5星")
    private Integer rating;

    @Schema(description = "评价内容")
    private String ratingComment;

    @Schema(description = "工单状态：0待审核 1待派单 2已派单 3维修中 4已完成 5已评价 6已驳回 7已取消")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "超时标记：0-正常 1-已超时")
    private Integer timeoutFlag;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
