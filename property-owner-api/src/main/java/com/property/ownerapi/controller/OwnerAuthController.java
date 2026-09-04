package com.property.ownerapi.controller;

import com.property.common.constant.RoleConstant;
import com.property.common.dto.LoginUser;
import com.property.common.result.ApiResult;
import com.property.framework.web.annotation.SkipAuth;
import com.property.framework.web.security.TokenCookieUtil;
import com.property.framework.web.security.TokenService;
import com.property.ownerapi.dto.request.OwnerLoginRequest;
import com.property.ownerapi.dto.response.OwnerLoginResponse;
import com.property.ownerapi.service.OwnerAuthService;
import com.wf.captcha.SpecCaptcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 业主端：登录/登出
 */
@Tag(name = "业主认证", description = "业主登录接口")
@RestController
@RequestMapping("/api/owner/auth")
@RequiredArgsConstructor
@SkipAuth
public class OwnerAuthController {

    private static final String CAPTCHA_SESSION_KEY = "owner_captcha";
    private static final String REFRESH_COOKIE_PATH = "/api/owner/auth/refresh";

    private final OwnerAuthService ownerAuthService;
    private final TokenService tokenService;

    @Operation(summary = "业主登录", description = "使用手机号、密码和验证码登录，返回JWT Token（同时写入httpOnly Cookie）")
    @PostMapping("/login")
    public ApiResult<OwnerLoginResponse> login(@Valid @RequestBody OwnerLoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse response) {
        // 1. 校验验证码（一次性，无论成败都移除）
        String sessionCaptcha = (String) httpRequest.getSession().getAttribute(CAPTCHA_SESSION_KEY);
        httpRequest.getSession().removeAttribute(CAPTCHA_SESSION_KEY);
        ownerAuthService.verifyCaptcha(sessionCaptcha, request.getCaptcha());

        // 2. 执行业务登录
        OwnerLoginResponse loginResponse = ownerAuthService.login(request);
        LoginUser loginUser = LoginUser.builder()
                .userId(loginResponse.getOwnerId())
                .username(loginResponse.getPhone())
                .realName(loginResponse.getOwnerName())
                .role(loginResponse.getRole())
                .build();
        TokenService.TokenPair pair = tokenService.issue(loginUser);
        TokenCookieUtil.writeAccessCookie(response, TokenCookieUtil.ACCESS_COOKIE, pair.accessToken(), tokenService.getAccessMaxAgeSeconds());
        TokenCookieUtil.writeRefreshCookie(response, TokenCookieUtil.REFRESH_COOKIE, pair.refreshToken(), REFRESH_COOKIE_PATH, tokenService.getRefreshMaxAgeSeconds());
        loginResponse.setToken(pair.accessToken());
        return ApiResult.success("登录成功", loginResponse);
    }

    @Operation(summary = "刷新 Access Token", description = "使用 Refresh Token 换取新的 Access Token（无感刷新）")
    @PostMapping("/refresh")
    public ApiResult<Void> refresh(@CookieValue(value = TokenCookieUtil.REFRESH_COOKIE, required = false) String refreshToken,
                                   HttpServletResponse response) {
        String newAccess = tokenService.refresh(refreshToken, RoleConstant.OWNER);
        if (newAccess == null) {
            return ApiResult.error(401, "刷新失败，请重新登录");
        }
        TokenCookieUtil.writeAccessCookie(response, TokenCookieUtil.ACCESS_COOKIE, newAccess, tokenService.getAccessMaxAgeSeconds());
        return ApiResult.success("刷新成功", null);
    }

    @Operation(summary = "业主登出", description = "清除登录Cookie")
    @PostMapping("/logout")
    public ApiResult<Void> logout(@CookieValue(value = TokenCookieUtil.ACCESS_COOKIE, required = false) String accessToken,
                                  HttpServletResponse response) {
        Long userId = null;
        try {
            if (accessToken != null) {
                userId = tokenService.getJwtUtilUserId(accessToken);
            }
        } catch (Exception ignored) {
        }
        tokenService.logout(accessToken, RoleConstant.OWNER, userId);
        TokenCookieUtil.clearCookie(response, TokenCookieUtil.ACCESS_COOKIE, "/");
        TokenCookieUtil.clearCookie(response, TokenCookieUtil.REFRESH_COOKIE, REFRESH_COOKIE_PATH);

        return ApiResult.success("登出成功", null);
    }

    @Operation(summary = "获取登录验证码", description = "生成图形验证码并写入 Session，返回 PNG 图片")
    @GetMapping("/captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SpecCaptcha specCaptcha = new SpecCaptcha(120, 40, 5);
        // 验证码文本存入 session（转小写，比较时忽略大小写）
        request.getSession().setAttribute(CAPTCHA_SESSION_KEY, specCaptcha.text().toLowerCase());
        response.setContentType("image/png");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setDateHeader("Expires", 0);
        specCaptcha.out(response.getOutputStream());
    }
}