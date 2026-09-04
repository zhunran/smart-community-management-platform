package com.property.module.parking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车位预警 VO
 */
@Data
public class ParkingWarningVO {

    @Schema(description = "预警ID")
    private Long id;

    @Schema(description = "车位ID")
    private Long spaceId;

    @Schema(description = "车位编号")
    private String spaceCode;

    @Schema(description = "车位名称")
    private String spaceName;

    @Schema(description = "预警类型")
    private String warningType;

    @Schema(description = "预警类型名称")
    private String warningTypeName;

    @Schema(description = "预警等级")
    private String warningLevel;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态：0-待处理 1-处理中 2-已处理 3-已关闭")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "处理人")
    private String handler;

    @Schema(description = "处理备注")
    private String handleRemark;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "对账批次号")
    private String batchNo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
