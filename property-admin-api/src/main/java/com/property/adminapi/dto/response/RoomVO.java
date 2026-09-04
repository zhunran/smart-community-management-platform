package com.property.adminapi.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房屋响应 VO
 */
@Data
public class RoomVO {

    private Long id;
    private Long buildingId;
    private Long unitId;
    private String roomCode;
    private String roomName;
    private Integer floor;
    private Integer roomType;
    private BigDecimal area;
    private BigDecimal usableArea;
    private String orientation;
    private Integer decorationStatus;
    private Integer occupancyStatus;
    private BigDecimal propertyFeeRate;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 关联楼栋名称（由 Service 层填充） */
    private String buildingName;

    /** 关联单元名称（由 Service 层填充） */
    private String unitName;
}
