package com.property.module.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 投票请求 DTO
 */
@Data
public class VoteCastRequest {

    @Schema(description = "所投选项ID列表（单选1个，多选1到N个）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "至少选择一个选项")
    private List<Long> optionIds;
}
