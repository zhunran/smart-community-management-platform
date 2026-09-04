package com.property.module.lifeservice.dto.repair.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairOrderAssignRequest {

    @NotNull(message = "维修员不能为空")
    @Schema(description = "维修员ID（sys_user）")
    private Long handlerId;
}
