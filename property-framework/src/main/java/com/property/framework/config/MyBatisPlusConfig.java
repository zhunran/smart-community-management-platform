package com.property.framework.config;

import java.time.LocalDateTime;

import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

/**
 * MyBatis-Plus 配置
 * 分页插件 + 自动填充 + Mapper 扫描
 * ※ 集中管理所有模块的 Mapper 包路径，新增模块时在此追加
 */
@Configuration
@MapperScan({
        "com.property.framework.repository",
        "com.property.ownerapi.repository",
        "com.property.module.owner.repository",
        "com.property.module.bill.repository",
        "com.property.module.parking.repository",
        "com.property.module.ai.repository",
        "com.property.module.notification.repository",
        "com.property.module.statistic.repository",
        "com.property.module.housing.repository",
        "com.property.module.community.repository",
        "com.property.module.lifeservice.repository"
})
public class MyBatisPlusConfig {

    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁：使 @Version 字段生效，updateById/update 时自动加 version 条件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * 自动填充处理器（createTime/updateTime/createBy/updateBy）
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "createBy", String.class, "system");
                this.strictInsertFill(metaObject, "updateBy", String.class, "system");
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                this.strictUpdateFill(metaObject, "updateBy", String.class, "system");
            }
        };
    }
}
