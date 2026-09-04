package com.property.framework.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 *
 * 封装常用 Redis 操作，包括：
 * - 基础 KV 操作（set/get/delete/expire/hasKey）
 * - 原子计数（incr/decr）
 * - 分布式锁（tryLock/unlock，基于 SET NX EX）
 *
 * 注意：分布式锁使用简单的 SET NX EX 实现，适用于本项目的低并发场景。
 * 如果未来需要高可靠分布式锁（可重入、红锁、自动续期），可引入 Redisson。
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 基础 KV 操作 ====================

    /** 设置值（永不过期） */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /** 设置值并指定过期时间 */
    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /** 设置值（秒级过期，便捷方法） */
    public void set(String key, Object value, long timeoutSeconds) {
        redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    /** 获取值 */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 获取值并指定类型 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    /** 删除单个 Key */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /** 批量删除 Key */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /** 判断 Key 是否存在 */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /** 设置过期时间 */
    public Boolean expire(String key, Duration timeout) {
        return redisTemplate.expire(key, timeout);
    }

    /** 获取剩余过期时间（秒） */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // ==================== 原子计数 ====================

    /** 自增 1 */
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /** 自增指定步长 */
    public Long incrBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /** 自减 1 */
    public Long decr(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    // ==================== 分布式锁 ====================

    /**
     * 尝试获取分布式锁（非阻塞）
     *
     * @param lockKey   锁 Key（如 "lock:payment:callback:PAY202608150001"）
     * @param requestId 持有者标识（建议用 UUID，用于安全释放锁）
     * @param timeout   锁过期时间（防止死锁）
     * @return true=获取成功，false=获取失败（锁已被其他线程持有）
     */
    public boolean tryLock(String lockKey, String requestId, Duration timeout) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, requestId, timeout);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放分布式锁
     *
     * 使用 Lua 脚本保证"判断持有者 + 删除 Key"的原子性，
     * 防止误删其他线程持有的锁。
     *
     * @param lockKey   锁 Key
     * @param requestId 持有者标识（必须与加锁时一致）
     * @return true=释放成功，false=锁已不属于当前持有者
     */
    public boolean unlock(String lockKey, String requestId) {
        String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "  return redis.call('del', KEYS[1]) " +
                "else " +
                "  return 0 " +
                "end";
        Long result = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                java.util.Collections.singletonList(lockKey),
                requestId);
        return result != null && result > 0;
    }
}
