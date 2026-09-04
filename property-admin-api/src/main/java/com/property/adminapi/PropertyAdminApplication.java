package com.property.adminapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 管理员 API 启动类
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "com.property.framework",
        "com.property.adminapi",
        "com.property.module.owner",
        "com.property.module.bill",
        "com.property.module.payment",
        "com.property.module.parking",
        "com.property.module.notification",
        "com.property.module.statistic",
        "com.property.module.housing",
        "com.property.module.lifeservice",
        "com.property.module.community"
})
public class PropertyAdminApplication {

    static void main(String[] args) {
        SpringApplication.run(PropertyAdminApplication.class, args);
    }
}
