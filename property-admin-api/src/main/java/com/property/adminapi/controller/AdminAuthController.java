package com.property.adminapi.controller;

import com.property.adminapi.dto.request.LoginRequest;
import com.property.adminapi.dto.response.LoginResponse;
import com.property.adminapi.service.AdminAuthService;
import com.property.common.dto.LoginUser;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.SkipAuth;
import com.property.framework.web.security.TokenCookieUtil;
import com.property.framework.web.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端：登录/登出
 */
@Tag(name = "管理员认证", description = "管理员登录接口")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@SkipAuth
public class AdminAuthController {

    private static final String REFRESH_COOKIE_PATH = "/api/admin/auth/refresh";

    private final AdminAuthService adminAuthService;
    private final TokenService tokenService;

    @Operation(summary = "管理员登录", description = "校验用户名密码，签发Access+Refresh双Token并写入httpOnly Cookie")
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = adminAuthService.login(request);
        LoginUser loginUser = LoginUser.builder()
                .userId(loginResponse.getUserId())
                .username(loginResponse.getUsername())
                .realName(loginResponse.getRealName())
                .role(loginResponse.getRole())
                .build();
        TokenService.TokenPair pair = tokenService.issue(loginUser);
        TokenCookieUtil.writeAccessCookie(response, TokenCookieUtil.ACCESS_COOKIE, pair.accessToken(), tokenService.getAccessMaxAgeSeconds());
        TokenCookieUtil.writeRefreshCookie(response, TokenCookieUtil.REFRESH_COOKIE, pair.refreshToken(), REFRESH_COOKIE_PATH, tokenService.getRefreshMaxAgeSeconds());
        loginResponse.setToken(pair.accessToken());
        return ApiResult.success("登录成功", loginResponse);
    }

    @Operation(summary = "刷新Access Token", description = "使用Refresh Token换取新的Access Token（无感刷新）")
    @PostMapping("/refresh")
    public ApiResult<Void> refresh(@CookieValue(value = TokenCookieUtil.REFRESH_COOKIE, required = false) String refreshToken,
                                   HttpServletResponse response) {
        String newAccess = adminAuthService.refresh(refreshToken);
        TokenCookieUtil.writeAccessCookie(response, TokenCookieUtil.ACCESS_COOKIE, newAccess, tokenService.getAccessMaxAgeSeconds());
        return ApiResult.success("刷新成功", null);
    }

    @Operation(summary = "管理员登出", description = "删除Refresh Token并将Access Token加入黑名单")
    @PostMapping("/logout")
    public ApiResult<Void> logout(@CookieValue(value = TokenCookieUtil.ACCESS_COOKIE, required = false) String accessToken,
                                  HttpServletResponse response) {
        adminAuthService.logout(accessToken);
        TokenCookieUtil.clearCookie(response, TokenCookieUtil.ACCESS_COOKIE, "/");
        TokenCookieUtil.clearCookie(response, TokenCookieUtil.REFRESH_COOKIE, REFRESH_COOKIE_PATH);
        return ApiResult.success("登出成功", null);
    }
}