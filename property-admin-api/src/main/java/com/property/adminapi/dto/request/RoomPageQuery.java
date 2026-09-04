package com.property.adminapi.dto.request;

import com.property.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房屋分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoomPageQuery extends PageQuery {

    private Long buildingId;

    private Long unitId;

    private String roomCode;

    private String roomName;

    private Integer floor;

    private Integer roomType;

    private Integer occupancyStatus;

    private Integer status;
}
