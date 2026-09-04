package com.property.module.community.dto.respose;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 投票选项 VO
 */
@Data
public class VoteOptionVO {

    @Schema(description = "选项ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "选项内容")
    private String content;

    @Schema(description = "票数")
    private Integer voteCount;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
