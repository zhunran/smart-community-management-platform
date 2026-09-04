package com.property.common.result;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页返回体
 */
@Data
public class PageResult<T> {

    private long total;
    private List<T> records;
    private long current;
    private long size;

    public PageResult() {
    }

    public PageResult(long total, List<T> records, long current, long size) {
        this.total = total;
        this.records = records;
        this.current = current;
        this.size = size;
    }

    /**
     * 创建空分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, Collections.emptyList(), 1L, 10L);
    }
}
