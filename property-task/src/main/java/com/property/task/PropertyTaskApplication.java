package com.property.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 定时任务执行器启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.property.common",
        "com.property.framework",
        "com.property.module.bill",
        "com.property.module.notification",
        "com.property.module.owner",
        "com.property.module.parking",
        "com.property.module.payment",
        "com.property.module.housing",
        "com.property.module.lifeservice",
        "com.property.adminapi",
        "com.property.ownerapi",
        "com.property.task"
})
public class PropertyTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(PropertyTaskApplication.class, args);
    }
}
