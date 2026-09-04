package com.property.module.community.dto.respose;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 论坛评论 VO（支持两级嵌套）
 */
@Data
public class ForumCommentVO {

    @Schema(description = "评论ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "帖子ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long postId;

    @Schema(description = "父评论ID（0=一级评论）")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentId;

    @Schema(description = "被回复人ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long replyTo;

    @Schema(description = "评论人ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ownerId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "状态：0-待审核 1-正常 2-已删除")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "子评论列表")
    private List<ForumCommentVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}