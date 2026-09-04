package com.property.module.community.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForumPostCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最多100字")
    @Schema(description = "帖子标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "图片URL列表，逗号分隔")
    private String images;

    @NotNull(message = "分类不能为空")
    @Schema(description = "帖子分类：1-二手 2-失物 3-装修 4-互助 5-其他")
    private Integer category;

    @Schema(description = "关联房屋ID")
    private Long roomId;
}