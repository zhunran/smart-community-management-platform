package com.property.module.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForumCommentCreateRequest {

    @NotNull(message = "帖子ID不能为空")
    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "父评论ID（0或null表示一级评论）")
    private Long parentId;

    @Schema(description = "被回复人ID（回复二级评论时使用）")
    private Long replyTo;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容")
    private String content;
}