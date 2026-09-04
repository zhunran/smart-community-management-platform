package com.property.framework.web.interceptor;

import com.property.common.dto.LoginUser;
import com.property.common.exception.AuthException;
import com.property.framework.web.annotation.SkipAuth;
import com.property.framework.web.security.JwtUtil;
import com.property.framework.web.security.SecurityUtil;
import com.property.framework.web.security.TokenCookieUtil;
import com.property.framework.web.security.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器
 * 解析 JWT Token，设置 LoginUser 到 ThreadLocal
 * 接口处理完毕后自动清理（在 afterCompletion 中执行）
 */
@Setter
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    /**
     * -- SETTER --
     *  延迟注入 JwtUtil（避免循环依赖）
     */
    private JwtUtil jwtUtil;
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 非 HandlerMethod（如静态资源），直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查 @SkipAuth 注解（方法 > 类）
        if (hasSkipAuth(handlerMethod)) {
            return true;
        }

        //解析Token
        String token=extractToken(request);
        if(token==null)
        {
            log.warn("未提供Token，URI={}",request.getRequestURI());
        }
        //黑名单检查：已登出的 Access Token 直接拒绝
        if (tokenService != null && tokenService.isBlacklisted(token))
        {
            log.warn("Token已失效（黑名单），拒绝访问 URI={}", request.getRequestURI());
            throw new AuthException("Token已失效，请重新登录");
        }
        try {
            //getLoginUser内部会校验type=access
            LoginUser loginUser=jwtUtil.getLoginUser(token);
            SecurityUtil.setLoginUser(loginUser);
        }catch (Exception e)
        {
            log.warn("Token解析失败，URI={}，error={}",request.getRequestURI(),e.getMessage());
            throw new AuthException("Token无效或者已经过期");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 请求结束清理 ThreadLocal，防止内存泄漏
        SecurityUtil.clear();
    }

    /**
     * 提取 Token：优先从 Authorization 请求头读取，其次从 Cookie 读取
     */
    private String extractToken(HttpServletRequest request) {
        // 1. 优先从 Authorization 头读取（向后兼容）
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length()).trim();
        }
        // 2. 从 Cookie 读取
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (TokenCookieUtil.ACCESS_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 检查方法或类上是否有 @SkipAuth 注解
     */
    private boolean hasSkipAuth(HandlerMethod handlerMethod) {
        // 方法上的 @SkipAuth 优先级最高
        if (handlerMethod.getMethodAnnotation(SkipAuth.class) != null) {
            return true;
        }
        // 类上的 @SkipAuth
        if (handlerMethod.getBeanType().getAnnotation(SkipAuth.class) != null) {
            return true;
        }
        return false;
    }
}
