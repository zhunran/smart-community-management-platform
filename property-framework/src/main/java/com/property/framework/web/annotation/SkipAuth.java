package com.property.framework.web.annotation;

import java.lang.annotation.*;

/**
 * 跳过鉴权注解
 * 标注在 Controller 方法或类上，表示该接口不需要登录即可访问
 * 优先级：方法 > 类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkipAuth {
}
