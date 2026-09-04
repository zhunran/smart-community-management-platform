package com.property.module.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新增社区投票请求 DTO
 */
@Data
public class VoteCreateRequest {

    @Schema(description = "投票标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "投票标题不能为空")
    @Size(max = 200, message = "投票标题长度不能超过200")
    private String title;

    @Schema(description = "投票描述")
    private String description;

    @Schema(description = "投票类型：1单选 2多选", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "投票类型不能为空")
    private Integer voteType;

    @Schema(description = "是否匿名：1匿名 0实名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否匿名不能为空")
    private Integer isAnonymous;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "投票选项内容列表（至少2项）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "投票选项不能为空")
    private List<@NotBlank(message = "选项内容不能为空") String> options;
}
