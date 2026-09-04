package com.property.module.lifeservice.dto.repair.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RepairOrderQuery extends PageQuery {

    @Schema(description = "报修分类：1-水电 2-门窗 3-电梯 4-公共设施 5-其他")
    private Integer category;

    @Schema(description = "工单状态：0待审核 1待派单 2已派单 3维修中 4已完成 5已评价 6已驳回 7已取消")
    private Integer status;

    @Schema(description = "超时标记：0-正常 1-已超时")
    private Integer timeoutFlag;

    @Schema(description = "关键词搜索（标题+工单号）")
    private String keyword;
}
