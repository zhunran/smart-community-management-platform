package com.property.ownerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 业主端启动类
 */
@SpringBootApplication(scanBasePackages = "com.property")
public class PropertyOwnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PropertyOwnerApplication.class, args);
    }
}
