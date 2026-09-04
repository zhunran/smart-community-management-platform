package com.property.framework.web.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Token Cookie 工具类
 * 统一管理 Access/Refresh Token 的 Cookie 写入与清除
 */
public final class TokenCookieUtil {

    public static final String ACCESS_COOKIE = "token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private TokenCookieUtil() {
    }

    /**
     * 写入 Access Token Cookie（path=/）
     */
    public static void writeAccessCookie(HttpServletResponse response, String name, String token, int maxAge) {
        Cookie cookie = new Cookie(name, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * 写入 Refresh Token Cookie（指定 path）
     */
    public static void writeRefreshCookie(HttpServletResponse response, String name, String token, String path, int maxAge) {
        Cookie cookie = new Cookie(name, token);
        cookie.setHttpOnly(true);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * 清除 Cookie（maxAge=0）
     */
    public static void clearCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setPath(path);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}