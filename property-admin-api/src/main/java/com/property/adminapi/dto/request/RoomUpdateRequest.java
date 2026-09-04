package com.property.adminapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 修改房屋请求 DTO
 */
@Data
public class RoomUpdateRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotNull(message = "所属楼栋不能为空")
    private Long buildingId;

    @NotNull(message = "所属单元不能为空")
    private Long unitId;

    @NotBlank(message = "房号不能为空")
    private String roomCode;

    @NotBlank(message = "房间名称不能为空")
    private String roomName;

    @NotNull(message = "所在楼层不能为空")
    private Integer floor;

    private Integer roomType;

    @NotNull(message = "建筑面积不能为空")
    private BigDecimal area;

    private BigDecimal usableArea;

    private String orientation;

    private Integer decorationStatus;

    private Integer occupancyStatus;

    private BigDecimal propertyFeeRate;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
