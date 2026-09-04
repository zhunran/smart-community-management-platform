package com.property.module.community.dto.respose;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 论坛帖子详情 VO
 */
@Data
public class ForumPostDetailVO {

    @Schema(description = "帖子ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "图片URL列表")
    private String images;

    @Schema(description = "分类：1-二手 2-失物 3-装修 4-互助 5-其他")
    private Integer category;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "发帖人ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ownerId;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "是否置顶")
    private Integer isPinned;

    @Schema(description = "是否加精")
    private Integer isEssence;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "敏感词")
    private String sensitiveWords;

    @Schema(description = "状态：0-待审核 1-已发布 2-已驳回 3-已删除")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "一级评论列表（含子评论）")
    private List<ForumCommentVO> comments;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}