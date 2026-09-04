package com.property.module.parking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 车位退租/解绑请求
 */
@Data
public class ParkingUnbindRequest {

    @NotNull(message = "车位ID不能为空")
    @Schema(description = "车位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long spaceId;

    @Schema(description = "退租备注")
    private String remark;
}
