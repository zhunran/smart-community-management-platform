package com.property.module.community.dto.respose;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 投票结果选项 VO（实名投票时带投票人明细）
 */
@Data
public class VoteResultOptionVO {

    @Schema(description = "选项ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long optionId;

    @Schema(description = "选项内容")
    private String content;

    @Schema(description = "票数")
    private Integer voteCount;

    @Schema(description = "投票人ID列表（仅实名投票返回）")
    private List<Long> ownerIds;
}
