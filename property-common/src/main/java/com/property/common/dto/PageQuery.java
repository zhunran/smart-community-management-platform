package com.property.common.dto;

import lombok.Data;

/**
 * 通用分页查询参数
 */
@Data
public class PageQuery {

    private long current = 1;
    private long size = 10;

    public long getCurrent() {
        return Math.max(current, 1);
    }

    public long getSize() {
        return Math.max(size, 1);
    }
}
