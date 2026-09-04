package com.property.module.lifeservice.dto.repair.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 工单统计 VO
 */
@Data
public class RepairStatisticsVO {

    @Schema(description = "工单总数")
    private Long total;

    @Schema(description = "各状态数量：key-状态值 value-数量（0待审核 1待派单 2已派单 3维修中 4已完成 5已评价 6已驳回 7已取消）")
    private Map<Integer, Long> statusCounts;

    @Schema(description = "平均处理时长（小时，已完成/已评价工单）")
    private BigDecimal avgHandleHours;
}
