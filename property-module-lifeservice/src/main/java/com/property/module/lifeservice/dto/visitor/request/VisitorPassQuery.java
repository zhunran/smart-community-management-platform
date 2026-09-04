package com.property.module.lifeservice.dto.visitor.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 访客通行码分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VisitorPassQuery extends PageQuery {

    @Schema(description = "状态：0有效 1已用尽 2已过期 3已撤销")
    private Integer status;

    @Schema(description = "关键词（访客姓名/手机号/车牌）")
    private String keyword;
}
