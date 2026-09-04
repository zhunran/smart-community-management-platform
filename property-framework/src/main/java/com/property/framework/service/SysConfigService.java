package com.property.framework.service;

import java.math.BigDecimal;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.property.framework.entity.SysConfigEntity;
import com.property.framework.repository.SysConfigMapper;

/**
 * 系统配置服务
 * 从 t_sys_config 表中读取配置项，提供类型安全的读取方法。
 * 配置项通过 Redis 缓存，默认 30 分钟，如需实时感知变化可调用 refreshCache() 失效。
 */
@Service
public class SysConfigService {

    private final SysConfigMapper sysConfigMapper;
    /** 自注入代理，解决 @Cacheable 内部调用绕过代理的问题 */
    private final SysConfigService self;

    public SysConfigService(SysConfigMapper sysConfigMapper, @Lazy SysConfigService self) {
        this.sysConfigMapper = sysConfigMapper;
        this.self = self;
    }

    /**
     * 获取字符串类型的配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值（数据库中不存在或禁用时返回）
     * @return 配置值
     */
    public String getString(String key, String defaultValue) {
        SysConfigEntity config = self.getConfig(key);
        if (config == null) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    /**
     * 获取整数类型的配置值
     */
    public int getInt(String key, int defaultValue) {
        SysConfigEntity config = self.getConfig(key);
        if (config == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(config.getConfigValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取 BigDecimal 类型的配置值
     */
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        SysConfigEntity config = self.getConfig(key);
        if (config == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(config.getConfigValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 查询配置项（按 key 精确查找，仅返回启用状态的）
     * 缓存策略：默认 30 分钟，配置变更时调用 refreshCache() 失效
     */
    @Cacheable(value = "sysConfig", key = "#key", unless = "#result == null")
    public SysConfigEntity getConfig(String key) {
        return sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfigEntity>()
                        .eq(SysConfigEntity::getConfigKey, key)
                        .eq(SysConfigEntity::getStatus, 1)
                        .last("LIMIT 1")
        );
    }

    /**
     * 刷新指定配置缓存（数据库配置变更后调用）
     */
    @CacheEvict(value = "sysConfig", key = "#key")
    public void refreshCache(String key) {
        // 仅触发缓存失效，方法体为空
    }
}
