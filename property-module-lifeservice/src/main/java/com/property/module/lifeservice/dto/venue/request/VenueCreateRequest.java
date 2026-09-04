package com.property.module.lifeservice.dto.venue.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 新增场地请求 DTO
 */
@Data
public class VenueCreateRequest {

    @Schema(description = "场地名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "场地名称不能为空")
    @Size(max = 100, message = "场地名称长度不能超过100")
    private String name;

    @Schema(description = "场地类型：1健身房 2棋牌室 3会议室 4游泳池 5其他", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "场地类型不能为空")
    private Integer venueType;

    @Schema(description = "场地位置")
    @Size(max = 200, message = "场地位置长度不能超过200")
    private String location;

    @Schema(description = "容量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "容量不能为空")
    private Integer capacity;

    @Schema(description = "开放时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开放时间不能为空")
    private LocalTime openTime;

    @Schema(description = "关闭时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "关闭时间不能为空")
    private LocalTime closeTime;

    @Schema(description = "预约粒度（分钟）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "预约粒度不能为空")
    private Integer slotMinutes;

    @Schema(description = "每业主每月上限（0=不限）")
    private Integer monthlyLimit;

    @Schema(description = "费用（0=免费）")
    private BigDecimal price;

    @Schema(description = "状态：0停用 1启用")
    private Integer status;
}
