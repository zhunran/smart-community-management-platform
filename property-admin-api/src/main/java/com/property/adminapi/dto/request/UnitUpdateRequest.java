package com.property.adminapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改单元请求 DTO
 */
@Data
public class UnitUpdateRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotNull(message = "所属楼栋不能为空")
    private Long buildingId;

    @NotBlank(message = "单元编号不能为空")
    private String unitCode;

    @NotBlank(message = "单元名称不能为空")
    private String unitName;

    private Integer totalFloors;

    private Integer totalRooms;

    private Integer elevatorCount;

    private Integer sortOrder;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
