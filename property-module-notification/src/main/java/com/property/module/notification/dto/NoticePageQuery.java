package com.property.module.notification.dto;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticePageQuery extends PageQuery {

    @Schema(description = "公告类型：NOTICE / WATER_ELECTRIC / ACTIVITY / EMERGENCY")
    private String type;

    @Schema(description = "状态：0-草稿 1-已发布 2-已下线")
    private Integer status;
}