package com.property.adminapi.dto.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单元分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UnitPageQuery extends PageQuery {

    @Schema(description = "楼栋ID（传0或不传表示不限制）")
    private Long buildingId;

    @Schema(description = "单元编号（模糊查询）")
    private String unitCode;

    @Schema(description = "单元名称（模糊查询）")
    private String unitName;

    @Schema(description = "状态：0-停用 1-启用（传空表示全部）")
    private Integer status;
}
