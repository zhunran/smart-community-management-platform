package com.property.adminapi.dto.response;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 收费报表导出 Excel VO
 */
@Data
public class ReportExcelVO {

    @ExcelProperty(value = "楼栋名称", index = 0)
    private String buildingName;

    @ExcelProperty(value = "单元名称", index = 1)
    private String unitName;

    @ExcelProperty(value = "房号", index = 2)
    private String roomCode;

    @ExcelProperty(value = "业主姓名", index = 3)
    private String ownerName;

    @ExcelProperty(value = "账单编号", index = 4)
    private String billNo;

    @ExcelProperty(value = "账期", index = 5)
    private String billPeriod;

    @ExcelProperty(value = "应收金额", index = 6)
    private String totalAmount;

    @ExcelProperty(value = "已交金额", index = 7)
    private String paidAmount;

    @ExcelProperty(value = "欠费金额", index = 8)
    private String arrearsAmount;

    @ExcelProperty(value = "滞纳金", index = 9)
    private String lateFee;

    @ExcelProperty(value = "缴费状态", index = 10)
    private String statusName;

    @ExcelProperty(value = "缴费截止日", index = 11)
    private String dueDate;
}
