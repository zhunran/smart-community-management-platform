package com.property.module.lifeservice.dto.visitor.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客通行码 VO
 */
@Data
public class VisitorPassVO {

    @Schema(description = "通行码ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Schema(description = "通行码（6位数字）")
    private String passCode;

    @Schema(description = "邀请人ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long ownerId;

    @Schema(description = "访客姓名")
    private String visitorName;

    @Schema(description = "访客手机号")
    private String visitorPhone;

    @Schema(description = "访客车牌")
    private String plateNo;

    @Schema(description = "生效时间")
    private LocalDateTime validFrom;

    @Schema(description = "失效时间")
    private LocalDateTime validUntil;

    @Schema(description = "可用次数（0=不限）")
    private Integer maxUse;

    @Schema(description = "已使用次数")
    private Integer usedCount;

    @Schema(description = "状态：0有效 1已用尽 2已过期 3已撤销")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
