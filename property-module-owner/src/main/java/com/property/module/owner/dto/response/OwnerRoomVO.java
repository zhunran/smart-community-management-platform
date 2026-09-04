package com.property.module.owner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 业主-房屋关联响应 VO
 */
@Data
public class OwnerRoomVO {

    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "业主ID")
    private Long ownerId;

    @Schema(description = "房屋ID")
    private Long roomId;

    @Schema(description = "关系类型：1-业主 2-家属 3-租客")
    private Integer relationType;

    @Schema(description = "是否主要业主：0-否 1-是")
    private Integer isPrimary;

    @Schema(description = "入住时间")
    private LocalDate moveInTime;

    @Schema(description = "搬离时间")
    private LocalDate moveOutTime;

    @Schema(description = "状态：0-无效 1-有效")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // ===== 以下由 Service 层填充 =====

    @Schema(description = "业主姓名")
    private String ownerName;

    @Schema(description = "业主手机号")
    private String ownerPhone;

    @Schema(description = "房屋编码（房号）")
    private String roomCode;

    @Schema(description = "房屋名称")
    private String roomName;

    @Schema(description = "楼栋名称")
    private String buildingName;

    @Schema(description = "单元名称")
    private String unitName;
}
