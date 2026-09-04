package com.property.framework.web.security;

import com.property.common.dto.LoginUser;
import com.property.common.exception.AuthException;

/**
 * 安全上下文工具
 * 基于 ThreadLocal 存储当前登录用户信息，供业务层随时获取
 */
public class SecurityUtil {

    private static final ThreadLocal<LoginUser> USER_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前登录用户
     */
    public static void setLoginUser(LoginUser loginUser) {
        USER_HOLDER.set(loginUser);
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {
        return USER_HOLDER.get();
    }

    /**
     * 获取当前用户ID（快捷方法）
     */
    public static Long getUserId() {
        LoginUser user = USER_HOLDER.get();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户名（快捷方法）
     */
    public static String getUsername() {
        LoginUser user = USER_HOLDER.get();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前用户角色（快捷方法）
     */
    public static String getRole() {
        LoginUser user = USER_HOLDER.get();
        return user != null ? user.getRole() : null;
    }

    /**
     * 获取当前用户，若为空则抛出鉴权异常
     */
    public static LoginUser requireUser() {
        LoginUser user = USER_HOLDER.get();
        if (user == null) {
            throw new AuthException("未登录或登录已过期");
        }
        return user;
    }

    /**
     * 清理当前用户（请求结束后必须调用）
     */
    public static void clear() {
        USER_HOLDER.remove();
    }
}
