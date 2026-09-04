package com.property.module.owner.dto.request;

import com.property.common.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 业主分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OwnerPageQuery extends PageQuery {

    @Schema(description = "业主姓名（模糊查询）", example = "张")
    private String ownerName;

    @Schema(description = "手机号（模糊查询）", example = "138")
    private String phone;

    @Schema(description = "证件类型：1-身份证 2-护照 3-港澳台证", example = "1")
    private Integer idCardType;

    @Schema(description = "证件号码（模糊查询）", example = "110101")
    private String idCardNo;

    @Schema(description = "性别：0-未知 1-男 2-女", example = "1")
    private Integer gender;

    @Schema(description = "业主类型：1-个人 2-公司 3-共有", example = "1")
    private Integer ownerType;

    @Schema(description = "状态：0-禁用 1-正常 2-冻结", example = "1")
    private Integer status;

    @Schema(description = "出生日期-开始（范围查询）", example = "1990-01-01")
    private LocalDate birthdayStart;

    @Schema(description = "出生日期-结束（范围查询）", example = "2000-12-31")
    private LocalDate birthdayEnd;

    @Schema(description = "注册时间-开始（范围查询）", example = "2024-01-01")
    private LocalDate registerTimeStart;

    @Schema(description = "注册时间-结束（范围查询）", example = "2024-12-31")
    private LocalDate registerTimeEnd;

    @Schema(description = "房号（模糊查询）", example = "101")
    private String roomCode;
}
