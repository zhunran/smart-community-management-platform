package com.property.module.lifeservice.dto.repair.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RepairOrderCompleteRequest {

    @NotBlank(message = "处理说明不能为空")
    @Schema(description = "处理说明")
    private String handleNote;
}
