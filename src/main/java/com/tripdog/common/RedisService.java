package com.tripdog.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis服务类
 * 提供基础的Redis操作方法
 *
 * @author tripdog
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    @Value("${redis.health-check.failure-threshold:3}")
    private int failureThreshold;

    @Value("${redis.health-check.key:redis:health:check}")
    private String healthCheckKey;

    /**
     * 设置key-value
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        if (!redisAvailable.get()) {
            log.debug("Redis不可用，跳过set: {}", key);
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value);
            markSuccess();
        } catch (Exception e) {
            markFailure("set", key, e);
        }
    }

    /**
     * 设置key-value并指定过期时间
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (!redisAvailable.get()) {
            log.debug("Redis不可用，跳过set: {}", key);
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            markSuccess();
        } catch (Exception e) {
            markFailure("setWithTimeout", key, e);
        }
    }

    /**
     * 获取value
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        if (!redisAvailable.get()) {
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(key);
            markSuccess();
            return value;
        } catch (Exception e) {
            markFailure("get", key, e);
            return null;
        }
    }

    /**
     * 获取字符串值
     * @param key 键
     * @return 字符串值
     */
    public String getString(String key) {
        if (!redisAvailable.get()) {
            return null;
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            markSuccess();
            return value;
        } catch (Exception e) {
            markFailure("getString", key, e);
            return null;
        }
    }

    /**
     * 设置字符串值
     * @param key 键
     * @param value 值
     */
    public void setString(String key, String value) {
        setString(key, value, -1, null);
    }

    /**
     * 设置字符串值并指定过期时间
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public boolean setString(String key, String value, long timeout, TimeUnit unit) {
        if (!redisAvailable.get()) {
            log.debug("Redis不可用，跳过setString: {}", key);
            return false;
        }
        try {
            if (timeout > 0 && unit != null) {
                stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
            } else {
                stringRedisTemplate.opsForValue().set(key, value);
            }
            markSuccess();
            return true;
        } catch (Exception e) {
            markFailure("setString", key, e);
            return false;
        }
    }

    /**
     * 检查Redis是否可用
     * @return Redis是否可用
     */
    public boolean isRedisAvailable() {
        return redisAvailable.get();
    }

    /**
     * 删除key
     * @param key 键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        if (!redisAvailable.get()) {
            return false;
        }
        try {
            Boolean deleted = redisTemplate.delete(key);
            markSuccess();
            return deleted;
        } catch (Exception e) {
            markFailure("delete", key, e);
            return false;
        }
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        if (!redisAvailable.get()) {
            return false;
        }
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            markSuccess();
            return hasKey;
        } catch (Exception e) {
            markFailure("hasKey", key, e);
            return false;
        }
    }

    /**
     * 设置key的过期时间
     * @param key 键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (!redisAvailable.get()) {
            return false;
        }
        try {
            Boolean result = redisTemplate.expire(key, timeout, unit);
            markSuccess();
            return result;
        } catch (Exception e) {
            markFailure("expire", key, e);
            return false;
        }
    }

    /**
     * 获取key的剩余过期时间
     * @param key 键
     * @param unit 时间单位
     * @return 剩余过期时间
     */
    public Long getExpire(String key, TimeUnit unit) {
        if (!redisAvailable.get()) {
            return -1L;
        }
        try {
            Long expire = redisTemplate.getExpire(key, unit);
            markSuccess();
            return expire;
        } catch (Exception e) {
            markFailure("getExpire", key, e);
            return -1L;
        }
    }

    /**
     * 原子性增加
     * @param key 键
     * @param delta 增加值
     * @return 增加后的值
     */
    public Long increment(String key, long delta) {
        if (!redisAvailable.get()) {
            return null;
        }
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            markSuccess();
            return result;
        } catch (Exception e) {
            markFailure("increment", key, e);
            return null;
        }
    }

    /**
     * 原子性减少
     * @param key 键
     * @param delta 减少值
     * @return 减少后的值
     */
    public Long decrement(String key, long delta) {
        if (!redisAvailable.get()) {
            return null;
        }
        try {
            Long result = redisTemplate.opsForValue().decrement(key, delta);
            markSuccess();
            return result;
        } catch (Exception e) {
            markFailure("decrement", key, e);
            return null;
        }
    }

    /**
     * 设置对象到Redis
     * @param key 键
     * @param obj 对象
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void setObject(String key, Object obj, long timeout, TimeUnit unit) {
        if (!redisAvailable.get()) {
            log.debug("Redis不可用，跳过setObject: {}", key);
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, obj, timeout, unit);
            markSuccess();
            log.debug("Redis setObject成功, key: {}", key);
        } catch (Exception e) {
            markFailure("setObject", key, e);
        }
    }

    /**
     * 从Redis获取对象
     * @param key 键
     * @param clazz 对象类型
     * @return 对象实例，如果不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, Class<T> clazz) {
        if (!redisAvailable.get()) {
            return null;
        }
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            markSuccess();
            if (obj == null) {
                return null;
            }

            if (clazz.isInstance(obj)) {
                return (T) obj;
            }

            log.debug("Redis getObject成功, key: {}", key);
            return (T) obj;
        } catch (Exception e) {
            markFailure("getObject", key, e);
            return null;
        }
    }

    @Scheduled(fixedDelayString = "${redis.health-check.interval:30000}")
    public void healthCheck() {
        if (redisAvailable.get()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().get(healthCheckKey);
            markSuccess();
            log.info("Redis健康检查成功，恢复连接");
        } catch (Exception e) {
            log.debug("Redis健康检查仍失败: {}", e.getMessage());
        }
    }

    private void markFailure(String operation, String key, Exception e) {
        log.error("Redis {} 操作失败, key={}, error={}", operation, key, e.getMessage());
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold && redisAvailable.compareAndSet(true, false)) {
            log.error("Redis连接连续失败{}次，标记为不可用", failures);
        }
    }

    private void markSuccess() {
        consecutiveFailures.set(0);
        if (!redisAvailable.get()) {
            redisAvailable.set(true);
            log.info("Redis连接恢复可用");
        }
    }
}
