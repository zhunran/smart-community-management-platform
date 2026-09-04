package com.property.module.bill.dto.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 费用项分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeItemPageQuery extends PageQuery {

    @Schema(description = "费用项编码（模糊查询）", example = "PROPERTY")
    private String itemCode;

    @Schema(description = "费用项名称（模糊查询）", example = "物业")
    private String itemName;

    @Schema(description = "计费周期：1-月 2-季 3-半年 4-年 5-一次性", example = "1")
    private Integer billingCycle;

    @Schema(description = "计费方式：1-面积 2-户 3-用量 4-固定金额", example = "1")
    private Integer calcType;

    @Schema(description = "状态：0-停用 1-启用", example = "1")
    private Integer status;
}
