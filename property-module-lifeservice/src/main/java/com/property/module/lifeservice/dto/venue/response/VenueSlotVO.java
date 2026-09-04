package com.property.module.lifeservice.dto.venue.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 场地时段占用 VO
 */
@Data
public class VenueSlotVO {

    @Schema(description = "场地ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long venueId;

    @Schema(description = "预约日期")
    private LocalDate date;

    @Schema(description = "开放时间")
    private LocalTime openTime;

    @Schema(description = "关闭时间")
    private LocalTime closeTime;

    @Schema(description = "预约粒度（分钟）")
    private Integer slotMinutes;

    @Schema(description = "已占用时段列表")
    private List<VenueOccupiedSlotVO> occupied;
}
