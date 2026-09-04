package com.property.module.lifeservice.dto.venue.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalTime;

/**
 * 场地已占用时段 VO
 */
@Data
public class VenueOccupiedSlotVO {

    @Schema(description = "开始时间")
    private LocalTime startTime;

    @Schema(description = "结束时间")
    private LocalTime endTime;
}
