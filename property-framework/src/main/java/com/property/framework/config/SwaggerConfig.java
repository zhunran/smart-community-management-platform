package com.property.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / Swagger 配置
 */
@Configuration
public class SwaggerConfig {

    @Value("${swagger.title:物业管理收费系统 API}")
    private String title;

    @Value("${swagger.version:2.0.0}")
    private String version;

    @Value("${swagger.description:物业管理收费系统后端接口文档}")
    private String description;

    @Value("${swagger.contact.name:物业系统开发团队}")
    private String contactName;

    @Value("${swagger.contact.email:dev@property.com}")
    private String contactEmail;
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version(version)
                        .description(description)
                        .contact(new Contact()
                                .name(contactName)
                                .email(contactEmail))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Token（由登录接口获取）\n"
                                        + "因认证方式为 HTTP Bearer，Knife4j 会自动添加 'Bearer ' 前缀，"
                                        + "输入框中只需填写 Token 原文，无需手动添加 'Bearer ' 前缀")));
    }

    /**
     * 管理员端 API 分组
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理员端")
                .displayName("管理员端 API")
                .pathsToMatch("/api/admin/**")
                .packagesToScan("com.property.adminapi.controller")
                .build();
    }

    /**
     * 业主端 API 分组
     * 当 property-owner-api 模块引入后自动生效（该模块的 controller 在 com.property.ownerapi.controller 包下）
     */
    @Bean
    @ConditionalOnExpression("T(org.springframework.util.ClassUtils).isPresent('com.property.ownerapi.controller.OwnerAuthController', null)")
    public GroupedOpenApi ownerApi() {
        return GroupedOpenApi.builder()
                .group("业主端")
                .displayName("业主端 API")
                .pathsToMatch("/api/owner/**")
                .packagesToScan("com.property.ownerapi.controller")
                .build();
    }
}
