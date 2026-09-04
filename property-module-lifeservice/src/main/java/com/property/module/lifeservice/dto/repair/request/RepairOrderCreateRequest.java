package com.property.module.lifeservice.dto.repair.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairOrderCreateRequest {

    @NotBlank(message = "问题描述不能为空")
    @Schema(description = "问题描述(简)")
    private String title;

    @NotBlank(message = "问题详情不能为空")
    @Schema(description = "问题详情")
    private String description;

    @Schema(description = "现场照片URL列表，逗号分隔")
    private String images;

    @NotNull(message = "报修分类不能为空")
    @Schema(description = "报修分类：1-水电 2-门窗 3-电梯 4-公共设施 5-其他")
    private Integer category;

    @NotNull(message = "紧急程度不能为空")
    @Schema(description = "紧急程度：1-普通 2-紧急 3-特急")
    private Integer urgency;

    @NotNull(message = "报修房屋不能为空")
    @Schema(description = "报修房屋ID")
    private Long roomId;
}
