package com.tripdog.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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

    /**
     * 设置key-value
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis set操作失败, key: {}", key, e);
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
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("Redis set with timeout操作失败, key: {}", key, e);
        }
    }

    /**
     * 获取value
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis get操作失败, key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取字符串值
     * @param key 键
     * @return 字符串值
     */
    public String getString(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis getString操作失败, key: {}", key, e);
            return null;
        }
    }

    /**
     * 设置字符串值
     * @param key 键
     * @param value 值
     */
    public void setString(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis setString操作失败, key: {}", key, e);
        }
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
        try {
            stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
            return true;
        } catch (Exception e) {
            log.error("Redis setString with timeout操作失败, key: {}", key, e);
            return false;
        }
    }

    /**
     * 检查Redis是否可用
     * @return Redis是否可用
     */
    public boolean isRedisAvailable() {
        try {
            // 尝试执行一个简单的Redis操作来验证连接
            stringRedisTemplate.opsForValue().get("redis:health:check");
            return true;
        } catch (Exception e) {
            log.debug("Redis连接检查失败", e);
            return false;
        }
    }

    /**
     * 删除key
     * @param key 键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis delete操作失败, key: {}", key, e);
            return false;
        }
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis hasKey操作失败, key: {}", key, e);
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
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis expire操作失败, key: {}", key, e);
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
        try {
            return redisTemplate.getExpire(key, unit);
        } catch (Exception e) {
            log.error("Redis getExpire操作失败, key: {}", key, e);
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
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis increment操作失败, key: {}", key, e);
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
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("Redis decrement操作失败, key: {}", key, e);
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
        try {
            redisTemplate.opsForValue().set(key, obj, timeout, unit);
            log.debug("Redis setObject成功, key: {}", key);
        } catch (Exception e) {
            log.error("Redis setObject操作失败, key: {}", key, e);
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
        try {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj == null) {
                return null;
            }

            // 如果对象类型匹配，直接返回
            if (clazz.isInstance(obj)) {
                return (T) obj;
            }

            log.debug("Redis getObject成功, key: {}", key);
            return (T) obj;
        } catch (Exception e) {
            log.error("Redis getObject操作失败, key: {}", key, e);
            return null;
        }
    }
}
