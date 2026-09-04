package com.property.module.statistic.vo;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {
    private Long id;
    private String traceId;
    private String userName;    // 操作者账号
    private String realName;    // 操作者姓名
    private String module;      // 模块
    private String action;      // 动作
    private String requestMethod;
    private String requestUrl;
    private String ipAddress;
    private Integer status;     // 1成功 0失败
    private Integer resultCode;
    private String resultMsg;
    private Long costTime;
    private LocalDateTime createTime;
}
