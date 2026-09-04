package com.property.module.community.dto.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ForumPostQuery extends PageQuery {

    @Schema(description = "帖子分类：1-二手 2-失物 3-装修 4-互助 5-其他")
    private Integer category;

    @Schema(description = "帖子状态：0-待审核 1-已发布 2-已驳回 3-已删除")
    private Integer status;

    @Schema(description = "关键词搜索（标题+内容）")
    private String keyword;
}