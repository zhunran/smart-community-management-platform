package com.property.module.owner.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 修改业主请求 DTO
 */
@Data
public class OwnerUpdateRequest {

    @Schema(description = "业主ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "业主姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "业主姓名不能为空")
    @Size(max = 100, message = "业主姓名长度不能超过100")
    private String ownerName;

    @Schema(description = "手机号", example = "13812345678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "证件类型：1-身份证 2-护照 3-港澳台证", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "证件类型不能为空")
    private Integer idCardType;

    @Schema(description = "证件号码", example = "110101199001011234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "证件号码不能为空")
    @Size(max = 50, message = "证件号码长度不能超过50")
    private String idCardNo;

    @Schema(description = "性别：0-未知 1-男 2-女", example = "1")
    private Integer gender;

    @Schema(description = "出生日期", example = "1990-01-01")
    private LocalDate birthday;

    @Schema(description = "电子邮箱", example = "zhangsan@example.com")
    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;

    @Schema(description = "紧急联系人", example = "李四")
    @Size(max = 50, message = "紧急联系人长度不能超过50")
    private String emergencyContact;

    @Schema(description = "紧急联系电话", example = "13912345678")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "紧急联系电话格式不正确")
    private String emergencyPhone;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    @Size(max = 500, message = "头像URL长度不能超过500")
    private String avatar;

    @Schema(description = "业主类型：1-个人 2-公司 3-共有", example = "1")
    private Integer ownerType;

    @Schema(description = "状态：0-禁用 1-正常 2-冻结", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
