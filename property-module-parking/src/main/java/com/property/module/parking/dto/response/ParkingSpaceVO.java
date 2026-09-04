package com.property.module.parking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车位 VO
 */
@Data
public class ParkingSpaceVO {

    @Schema(description = "车位ID")
    private Long id;

    @Schema(description = "车位编号")
    private String spaceCode;

    @Schema(description = "车位名称")
    private String spaceName;

    @Schema(description = "车位类型：1-标准 2-子母 3-机械 4-充电桩")
    private Integer spaceType;

    @Schema(description = "面积")
    private BigDecimal area;

    @Schema(description = "楼层")
    private String floor;

    @Schema(description = "区域")
    private String zone;

    @Schema(description = "当前业主ID")
    private Long ownerId;

    @Schema(description = "业主姓名")
    private String ownerName;

    @Schema(description = "关联房屋ID")
    private Long roomId;

    @Schema(description = "房号")
    private String roomCode;

    @Schema(description = "使用方式：1-自有 2-租赁 3-临时")
    private Integer rentalType;

    @Schema(description = "月租费用")
    private BigDecimal monthlyFee;

    @Schema(description = "状态：0-空闲 1-已售 2-已租 3-临时 4-维修中")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
