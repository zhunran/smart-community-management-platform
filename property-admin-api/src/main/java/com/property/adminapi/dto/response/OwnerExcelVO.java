package com.property.adminapi.dto.response;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 业主导入导出 Excel VO
 * 导出时包含所有字段，导入时仅需部分字段
 */
@Data
public class OwnerExcelVO {

    // ========== 导入/导出共有字段 ==========

    @ExcelProperty(value = "业主姓名", index = 0)
    private String ownerName;

    @ExcelProperty(value = "手机号", index = 1)
    private String phone;

    @ExcelProperty(value = "证件类型", index = 2)
    private String idCardTypeLabel;

    @ExcelProperty(value = "证件号码", index = 3)
    private String idCardNo;

    @ExcelProperty(value = "性别", index = 4)
    private String genderLabel;

    @ExcelProperty(value = "出生日期", index = 5)
    private String birthday;

    @ExcelProperty(value = "电子邮箱", index = 6)
    private String email;

    @ExcelProperty(value = "紧急联系人", index = 7)
    private String emergencyContact;

    @ExcelProperty(value = "紧急联系电话", index = 8)
    private String emergencyPhone;

    @ExcelProperty(value = "业主类型", index = 9)
    private String ownerTypeLabel;

    @ExcelProperty(value = "状态", index = 10)
    private String statusLabel;

    // ========== 仅导出字段 ==========

    @ExcelProperty(value = "注册时间", index = 11)
    private String registerTime;

    @ExcelProperty(value = "创建时间", index = 12)
    private String createTime;

    // ========== 内部使用，不导出到 Excel ==========

    @ExcelIgnore
    private Integer idCardType;

    @ExcelIgnore
    private Integer gender;

    @ExcelIgnore
    private Integer ownerType;

    @ExcelIgnore
    private Integer status;

    /** 导入校验错误信息 */
    @ExcelIgnore
    private String errorMsg;

    /** 导入是否校验通过 */
    @ExcelIgnore
    private boolean valid = true;
}
