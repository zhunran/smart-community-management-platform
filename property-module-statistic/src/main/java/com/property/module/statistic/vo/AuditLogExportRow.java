package com.property.module.statistic.vo;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计导出行模型
 *
 * 独立于 {@link AuditLogVO}，避免 VO 与导出模型耦合。
 */
@Data
@ColumnWidth(18)
public class AuditLogExportRow {
    @ExcelProperty("操作时间")
    private LocalDateTime createTime;

    @ExcelProperty("操作账号")
    private String userName;

    @ExcelProperty("操作者姓名")
    private String realName;

    @ExcelProperty("模块")
    private String module;

    @ExcelProperty("动作")
    private String action;

    @ExcelProperty("请求方法")
    private String requestMethod;

    @ExcelProperty("请求地址")
    @ColumnWidth(36)
    private String requestUrl;

    @ExcelProperty("IP地址")
    private String ipAddress;

    @ExcelProperty("状态")
    private String statusText;

    @ExcelProperty("耗时(ms)")
    private Long costTime;

    @ExcelProperty("结果说明")
    @ColumnWidth(28)
    private String resultMsg;
}