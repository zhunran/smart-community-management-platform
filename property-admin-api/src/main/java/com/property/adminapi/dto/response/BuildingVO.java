package com.property.adminapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 楼栋响应 VO
 */
@Data
public class BuildingVO {

    private Long id;
    private String buildingCode;
    private String buildingName;
    private Integer totalUnits;
    private Integer totalFloors;
    private Integer totalRooms;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
