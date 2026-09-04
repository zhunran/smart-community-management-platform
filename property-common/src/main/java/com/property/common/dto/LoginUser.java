package com.property.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录用户信息（放入 ThreadLocal）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 角色标识（admin / owner） */
    private String role;

    /** 权限列表 */
    private List<String> permissions;

    /** 关联房屋ID（业主端适用） */
    private Long roomId;

    /** Token */
    private String token;
}
