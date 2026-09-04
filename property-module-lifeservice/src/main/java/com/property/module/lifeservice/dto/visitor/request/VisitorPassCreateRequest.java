package com.property.module.lifeservice.dto.visitor.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生成访客通行码请求 DTO
 */
@Data
public class VisitorPassCreateRequest {

    @Schema(description = "访客姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "访客姓名不能为空")
    @Size(max = 50, message = "访客姓名长度不能超过50")
    private String visitorName;

    @Schema(description = "访客手机号")
    @Size(max = 20, message = "访客手机号长度不能超过20")
    private String visitorPhone;

    @Schema(description = "访客车牌")
    @Size(max = 20, message = "访客车牌长度不能超过20")
    private String plateNo;

    @Schema(description = "生效时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生效时间不能为空")
    private LocalDateTime validFrom;

    @Schema(description = "失效时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "失效时间不能为空")
    private LocalDateTime validUntil;

    @Schema(description = "可用次数（0=不限）")
    private Integer maxUse;
}
