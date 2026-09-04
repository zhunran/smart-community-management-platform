package com.property.module.lifeservice.dto.venue.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 场地分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VenueQuery extends PageQuery {

    @Schema(description = "场地名称（模糊查询）")
    private String name;

    @Schema(description = "场地类型：1健身房 2棋牌室 3会议室 4游泳池 5其他")
    private Integer venueType;

    @Schema(description = "状态：0停用 1启用")
    private Integer status;
}
