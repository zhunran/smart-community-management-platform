package com.property.module.owner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 业主详情 VO（管理员可见敏感字段）
 */
@Data
public class OwnerDetailVO {

    @Schema(description = "业主ID")
    private Long id;

    @Schema(description = "业主姓名")
    private String ownerName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "证件类型：1-身份证 2-护照 3-港澳台证")
    private Integer idCardType;

    @Schema(description = "证件号码")
    private String idCardNo;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender;

    @Schema(description = "出生日期")
    private LocalDate birthday;

    @Schema(description = "电子邮箱")
    private String email;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    private String emergencyPhone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "业主类型：1-个人 2-公司 3-共有")
    private Integer ownerType;

    @Schema(description = "状态：0-禁用 1-正常 2-冻结")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "注册时间")
    private LocalDateTime registerTime;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
