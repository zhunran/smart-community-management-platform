package com.property.module.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForumLikeRequest {

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID（帖子ID或评论ID）")
    private Long targetId;

    @NotNull(message = "目标类型不能为空")
    @Schema(description = "目标类型：1-帖子 2-评论")
    private Integer targetType;
}