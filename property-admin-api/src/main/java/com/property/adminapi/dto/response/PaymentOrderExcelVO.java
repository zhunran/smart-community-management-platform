package com.property.adminapi.dto.response;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 缴费记录导出 Excel VO
 */
@Data
public class PaymentOrderExcelVO {

    @ExcelProperty(value = "支付单号", index = 0)
    private String paymentNo;

    @ExcelProperty(value = "账单编号", index = 1)
    private String billNo;

    @ExcelProperty(value = "账期", index = 2)
    private String billPeriod;

    @ExcelProperty(value = "楼栋名称", index = 3)
    private String buildingName;

    @ExcelProperty(value = "房号", index = 4)
    private String roomCode;

    @ExcelProperty(value = "业主姓名", index = 5)
    private String ownerName;

    @ExcelProperty(value = "业主手机号", index = 6)
    private String ownerPhone;

    @ExcelProperty(value = "付款人", index = 7)
    private String payerName;

    @ExcelProperty(value = "支付方式", index = 8)
    private String paymentMethodName;

    @ExcelProperty(value = "支付金额", index = 9)
    private String paymentAmount;

    @ExcelProperty(value = "支付状态", index = 10)
    private String paymentStatusName;

    @ExcelProperty(value = "支付时间", index = 11)
    private String paymentTime;

    @ExcelProperty(value = "第三方流水号", index = 12)
    private String transactionId;

    @ExcelProperty(value = "备注", index = 13)
    private String remark;
}
