package com.property.module.community.dto.respose;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 社区投票结果 VO
 */
@Data
public class VoteResultVO {

    @Schema(description = "投票ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long voteId;

    @Schema(description = "投票标题")
    private String title;

    @Schema(description = "投票类型：1单选 2多选")
    private Integer voteType;

    @Schema(description = "是否匿名：1匿名 0实名")
    private Integer isAnonymous;

    @Schema(description = "总票数")
    private Integer totalVotes;

    @Schema(description = "各选项投票结果")
    private List<VoteResultOptionVO> options;
}
