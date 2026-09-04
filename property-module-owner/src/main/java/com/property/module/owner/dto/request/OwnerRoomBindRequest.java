package com.property.module.owner.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 业主-房屋绑定请求 DTO
 */
@Data
public class OwnerRoomBindRequest {

    @Schema(description = "业主ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业主ID不能为空")
    private Long ownerId;

    @Schema(description = "房屋ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "房屋ID不能为空")
    private Long roomId;

    @Schema(description = "关系类型：1-业主 2-家属 3-租客", example = "1")
    private Integer relationType;

    @Schema(description = "是否主要业主：0-否 1-是", example = "1")
    private Integer isPrimary;

    @Schema(description = "入住时间", example = "2024-01-01")
    private LocalDate moveInTime;
}
