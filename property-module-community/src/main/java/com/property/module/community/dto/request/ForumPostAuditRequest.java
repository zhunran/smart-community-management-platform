package com.property.module.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForumPostAuditRequest {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：1-通过 2-驳回")
    private Integer status;

    @Schema(description = "驳回原因（驳回时必填）")
    private String rejectReason;
}