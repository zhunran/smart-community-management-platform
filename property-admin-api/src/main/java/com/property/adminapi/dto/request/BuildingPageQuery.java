package com.property.adminapi.dto.request;

import com.property.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 楼栋分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BuildingPageQuery extends PageQuery {

    private String buildingCode;

    private String buildingName;

    private Integer status;
}
