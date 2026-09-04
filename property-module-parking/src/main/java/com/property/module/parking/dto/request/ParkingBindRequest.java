package com.property.module.parking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 车位绑定请求
 */
@Data
public class ParkingBindRequest {

    @NotNull(message = "车位ID不能为空")
    @Schema(description = "车位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long spaceId;

    @NotNull(message = "业主ID不能为空")
    @Schema(description = "业主ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long ownerId;

    @Schema(description = "关联房屋ID")
    private Long roomId;

    @NotNull(message = "使用方式不能为空")
    @Schema(description = "使用方式：1-自有 2-租赁 3-临时", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rentalType;

    @Schema(description = "备注")
    private String remark;
}
