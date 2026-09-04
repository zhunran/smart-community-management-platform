package com.property.module.lifeservice.dto.venue.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 场地 VO
 */
@Data
public class VenueVO {

    @Schema(description = "场地ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "场地名称")
    private String name;

    @Schema(description = "场地类型：1健身房 2棋牌室 3会议室 4游泳池 5其他")
    private Integer venueType;

    @Schema(description = "场地类型名称")
    private String venueTypeName;

    @Schema(description = "场地位置")
    private String location;

    @Schema(description = "容量")
    private Integer capacity;

    @Schema(description = "开放时间")
    private LocalTime openTime;

    @Schema(description = "关闭时间")
    private LocalTime closeTime;

    @Schema(description = "预约粒度（分钟）")
    private Integer slotMinutes;

    @Schema(description = "每业主每月上限（0=不限）")
    private Integer monthlyLimit;

    @Schema(description = "费用（0=免费）")
    private BigDecimal price;

    @Schema(description = "状态：0停用 1启用")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;
}
