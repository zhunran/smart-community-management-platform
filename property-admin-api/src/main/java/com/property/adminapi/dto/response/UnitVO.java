package com.property.adminapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单元响应 VO
 */
@Data
public class UnitVO {

    private Long id;
    private Long buildingId;
    private String unitCode;
    private String unitName;
    private Integer totalFloors;
    private Integer totalRooms;
    private Integer elevatorCount;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 关联楼栋名称（由 Service 层填充） */
    private String buildingName;
}
