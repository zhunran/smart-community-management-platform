package com.property.module.lifeservice.dto.visitor.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 访客通行码核销请求 DTO
 */
@Data
public class VisitorPassVerifyRequest {

    @Schema(description = "通行码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "通行码不能为空")
    private String passCode;
}
