package com.property.framework.web.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作模块，如：业主管理 */
    String module() default "";
    /** 操作动作，如：新增业主 */
    String action() default "";
}
