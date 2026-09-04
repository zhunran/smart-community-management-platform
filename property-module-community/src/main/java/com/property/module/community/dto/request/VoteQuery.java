package com.property.module.community.dto.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 社区投票分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VoteQuery extends PageQuery {

    @Schema(description = "投票标题（模糊查询）")
    private String title;

    @Schema(description = "投票状态：0未开始 1进行中 2已结束")
    private Integer status;
}
