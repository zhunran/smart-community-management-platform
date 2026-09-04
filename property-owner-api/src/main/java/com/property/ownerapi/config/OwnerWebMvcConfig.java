package com.property.ownerapi.config;

import com.property.framework.web.interceptor.AuthInterceptor;
import com.property.framework.web.security.JwtUtil;
import com.property.framework.web.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 业主端 WebMVC 配置
 * 注册拦截器路径（/api/owner/**）
 */
@Configuration
@RequiredArgsConstructor
public class OwnerWebMvcConfig implements WebMvcConfigurer {

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
                .addPathPatterns("/api/owner/**")
                .order(10);
    }
}
