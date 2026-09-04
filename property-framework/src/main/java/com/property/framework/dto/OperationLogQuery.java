package com.property.framework.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogQuery {
    private LocalDateTime start;      // 开始时间
    private LocalDateTime end;        // 结束时间
    private Integer status;           // 状态
    private String module;            // 模块名
    private String keyword;           // 关键词
    private String userName;          // 登录名
    private Integer pageNum = 1;      // 当前页
    private Integer pageSize = 10;    // 每页大小
}
