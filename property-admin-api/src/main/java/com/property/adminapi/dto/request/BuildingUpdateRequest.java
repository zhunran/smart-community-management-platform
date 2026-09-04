package com.property.adminapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改楼栋请求 DTO
 */
@Data
public class BuildingUpdateRequest {

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotBlank(message = "楼栋编号不能为空")
    private String buildingCode;

    @NotBlank(message = "楼栋名称不能为空")
    private String buildingName;

    private Integer totalUnits;

    private Integer totalFloors;

    private Integer totalRooms;

    private Integer sortOrder;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
