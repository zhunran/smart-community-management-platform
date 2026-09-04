package com.property.module.community.dto.respose;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区投票详情 VO
 */
@Data
public class VoteDetailVO {

    @Schema(description = "投票ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "投票标题")
    private String title;

    @Schema(description = "投票描述")
    private String description;

    @Schema(description = "投票类型：1单选 2多选")
    private Integer voteType;

    @Schema(description = "投票类型名称")
    private String voteTypeName;

    @Schema(description = "是否匿名：1匿名 0实名")
    private Integer isAnonymous;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "投票状态：0未开始 1进行中 2已结束")
    private Integer status;

    @Schema(description = "投票状态名称")
    private String statusName;

    @Schema(description = "总票数")
    private Integer totalVotes;

    @Schema(description = "选项列表（含实时票数）")
    private List<VoteOptionVO> options;

    @Schema(description = "当前用户已投的选项ID（业主端详情用）")
    private List<Long> myVotedOptionIds;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
