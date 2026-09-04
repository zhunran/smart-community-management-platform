package com.property.module.parking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 车位变更请求
 */
@Data
public class ParkingChangeRequest {

    @NotNull(message = "车位ID不能为空")
    @Schema(description = "车位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long spaceId;

    @NotNull(message = "新业主ID不能为空")
    @Schema(description = "新业主ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long newOwnerId;

    @Schema(description = "新关联房屋ID")
    private Long newRoomId;

    @Schema(description = "备注")
    private String remark;
}
