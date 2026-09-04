package com.property.module.lifeservice.dto.visitor.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 访客通行码核销结果 VO
 */
@Data
public class VisitorPassVerifyVO {

    @Schema(description = "是否核销通过")
    private Boolean valid;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "通行码")
    private String passCode;

    @Schema(description = "访客姓名")
    private String visitorName;

    @Schema(description = "访客车牌")
    private String plateNo;

    @Schema(description = "已使用次数")
    private Integer usedCount;

    @Schema(description = "可用次数（0=不限）")
    private Integer maxUse;
}
