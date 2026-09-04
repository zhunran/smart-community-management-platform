package com.property.module.lifeservice.dto.venue.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 场地预约请求 DTO
 */
@Data
public class VenueBookingRequest {

    @Schema(description = "预约日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "预约日期不能为空")
    private LocalDate bookingDate;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;
}
