package com.property.ownerapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业主登录响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerLoginResponse {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "业主ID")
    private Long ownerId;

    @Schema(description = "业主姓名")
    private String ownerName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "角色标识（固定为 owner）")
    private String role;
}
