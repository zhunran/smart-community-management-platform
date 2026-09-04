package com.property.adminapi.dto.response;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 缴费记录导入 Excel VO
 *
 * Excel 列顺序：
 * 账单编号、支付方式、付款人、支付时间、备注
 */
@Data
public class PaymentOrderImportVO {

    @ExcelProperty(value = "账单编号", index = 0)
    private String billNo;

    @ExcelProperty(value = "支付方式", index = 1)
    private String paymentMethodLabel;

    @ExcelProperty(value = "付款人", index = 2)
    private String payerName;

    @ExcelProperty(value = "支付时间", index = 3)
    private String paymentTime;

    @ExcelProperty(value = "备注", index = 4)
    private String remark;

    // ========== 内部使用 ==========

    @ExcelIgnore
    private Integer paymentMethod;

    @ExcelIgnore
    private String errorMsg;

    @ExcelIgnore
    private boolean valid = true;
}
