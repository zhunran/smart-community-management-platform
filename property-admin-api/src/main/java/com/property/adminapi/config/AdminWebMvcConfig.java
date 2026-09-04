package com.property.adminapi.config;

import com.property.framework.web.interceptor.AuthInterceptor;
import com.property.framework.web.security.JwtUtil;
import com.property.framework.web.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 管理员端 WebMVC 配置
 * 注册拦截器路径（/api/admin/**）
 */
@Configuration
@RequiredArgsConstructor
public class AdminWebMvcConfig implements WebMvcConfigurer {//在请求到达控制器执勤啊进行拦截，用于校验用户身份，实现认证/授权

    private final AuthInterceptor authInterceptor;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注入 JwtUtil 到 AuthInterceptor
        authInterceptor.setJwtUtil(jwtUtil);
        // 注入 TokenService 到 AuthInterceptor（用于 Access Token 黑名单检查）
        authInterceptor.setTokenService(tokenService);

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/admin/**")
                .order(10);
    }
}
