package com.property.module.parking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 预警处理请求
 */
@Data
public class WarningHandleRequest {

    @NotBlank(message = "处理备注不能为空")
    @Schema(description = "处理备注", requiredMode = Schema.RequiredMode.REQUIRED)
    private String handleRemark;
}
