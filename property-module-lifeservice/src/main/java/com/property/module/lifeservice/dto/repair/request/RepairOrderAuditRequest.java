package com.property.module.lifeservice.dto.repair.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairOrderAuditRequest {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：true-通过 false-驳回")
    private Boolean approved;

    @Schema(description = "驳回原因（驳回时必填）")
    private String reason;
}
