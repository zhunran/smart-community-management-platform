package com.property.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置（CORS）
 * 允许前端跨域访问后端接口
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许携带凭证（Cookie、Authorization 头）
        config.setAllowCredentials(true);

        // 允许的来源域名（生产环境应替换为具体域名）
        config.addAllowedOriginPattern("*");

        // 允许的请求头
        config.addAllowedHeader("*");

        // 允许的 HTTP 方法
        config.addAllowedMethod("*");

        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        // 暴露响应头（前端可获取）
        config.addExposedHeader("X-Trace-Id");
        config.addExposedHeader("Content-Disposition");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
