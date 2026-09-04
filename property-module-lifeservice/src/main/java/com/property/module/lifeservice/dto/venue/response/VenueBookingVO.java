package com.property.module.lifeservice.dto.venue.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场地预约 VO
 */
@Data
public class VenueBookingVO {

    @Schema(description = "预约ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "场地ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long venueId;

    @Schema(description = "场地名称")
    private String venueName;

    @Schema(description = "业主ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ownerId;

    @Schema(description = "预约日期")
    private LocalDate bookingDate;

    @Schema(description = "开始时间")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    private LocalTime endTime;

    @Schema(description = "状态：0已预约 1已使用 2已取消 3已违约")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
