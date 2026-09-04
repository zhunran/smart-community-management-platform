package com.property.module.lifeservice.dto.repair.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairOrderRateRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    @Schema(description = "评价1-5星")
    private Integer rating;

    @Schema(description = "评价内容")
    private String ratingComment;
}
