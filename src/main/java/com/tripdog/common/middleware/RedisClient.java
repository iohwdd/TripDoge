package com.tripdog.common.middleware;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 客户端封装
 * 提供常用的 Redis 操作方法
 */
@Component
@RequiredArgsConstructor
public class RedisClient {
    private final RedisTemplate<String, Object> redisTemplate;

    // ========================== String 命令 ==========================

    /**
     * 设置缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存值，带过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存值并强制转换
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除指定 key
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 删除多个 key
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取 key 的过期时间（秒）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 设置 key 的过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 移除 key 的过期时间（持久化）
     */
    public Boolean persist(String key) {
        return redisTemplate.persist(key);
    }

    /**
     * 对 key 的值进行递增
     */
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 对 key 的值进行递增（指定步长）
     */
    public Long incrBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 对 key 的值进行递减
     */
    public Long decr(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 对 key 的值进行递减（指定步长）
     */
    public Long decrBy(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * 获取字符串长度
     */
    public Long strlen(String key) {
        return redisTemplate.opsForValue().size(key);
    }

    /**
     * 追加字符串
     */
    public Integer append(String key, String value) {
        return redisTemplate.opsForValue().append(key, value);
    }

    // ========================== Hash 命令 ==========================

    /**
     * 设置哈希字段
     */
    public void hset(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 批量设置哈希字段
     */
    public void hmset(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取哈希字段值
     */
    public Object hget(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 获取所有哈希字段值
     */
    public Map<Object, Object> hgetall(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除哈希字段
     */
    public Long hdel(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * 判断哈希字段是否存在
     */
    public Boolean hexists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * 获取哈希表中字段的个数
     */
    public Long hlen(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    /**
     * 获取所有哈希字段名
     */
    public Set<Object> hkeys(String key) {
        return redisTemplate.opsForHash().keys(key);
    }

    /**
     * 获取所有哈希字段值
     */
    public List<Object> hvals(String key) {
        return redisTemplate.opsForHash().values(key);
    }

    /**
     * 对哈希字段进行递增
     */
    public Long hincrby(String key, String field, long delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    /**
     * 对哈希字段进行递增（浮点数）
     */
    public Double hincrbyfloat(String key, String field, double delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    // ========================== List 命令 ==========================

    /**
     * 从左边推入元素
     */
    public Long lpush(String key, Object... values) {
        return redisTemplate.opsForList().leftPushAll(key, values);
    }

    /**
     * 从右边推入元素
     */
    public Long rpush(String key, Object... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    /**
     * 从左边弹出元素
     */
    public Object lpop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 从右边弹出元素
     */
    public Object rpop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取列表长度
     */
    public Long llen(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 获取列表范围内的元素
     */
    public List<Object> lrange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表指定位置的元素
     */
    public Object lindex(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    /**
     * 设置列表指定位置的元素
     */
    public void lset(String key, long index, Object value) {
        redisTemplate.opsForList().set(key, index, value);
    }

    /**
     * 删除列表中指定值的元素
     */
    public Long lrem(String key, long count, Object value) {
        return redisTemplate.opsForList().remove(key, count, value);
    }

    /**
     * 截断列表到指定范围
     */
    public void ltrim(String key, long start, long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    // ========================== Set 命令 ==========================

    /**
     * 添加集合成员
     */
    public Long sadd(String key, Object... members) {
        return redisTemplate.opsForSet().add(key, members);
    }

    /**
     * 删除集合成员
     */
    public Long srem(String key, Object... members) {
        return redisTemplate.opsForSet().remove(key, members);
    }

    /**
     * 获取集合所有成员
     */
    public Set<Object> smembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 判断集合成员是否存在
     */
    public Boolean sismember(String key, Object member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * 获取集合大小
     */
    public Long scard(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 随机获取集合成员
     */
    public Object srandmember(String key) {
        return redisTemplate.opsForSet().randomMember(key);
    }

    /**
     * 随机获取指定数量的集合成员
     */
    public List<Object> srandmember(String key, long count) {
        return redisTemplate.opsForSet().randomMembers(key, count);
    }

    /**
     * 随机删除并返回集合成员
     */
    public Object spop(String key) {
        return redisTemplate.opsForSet().pop(key);
    }

    /**
     * 集合交集
     */
    public Set<Object> sinter(String key1, String key2) {
        return redisTemplate.opsForSet().intersect(key1, key2);
    }

    /**
     * 集合并集
     */
    public Set<Object> sunion(String key1, String key2) {
        return redisTemplate.opsForSet().union(key1, key2);
    }

    /**
     * 集合差集
     */
    public Set<Object> sdiff(String key1, String key2) {
        return redisTemplate.opsForSet().difference(key1, key2);
    }

    // ========================== ZSet 命令 ==========================

    /**
     * 添加有序集合成员
     */
    public Boolean zadd(String key, Object member, double score) {
        return redisTemplate.opsForZSet().add(key, member, score);
    }

    /**
     * 批量添加有序集合成员
     */
    public Long zadd(String key, Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples) {
        return redisTemplate.opsForZSet().add(key, tuples);
    }

    /**
     * 删除有序集合成员
     */
    public Long zrem(String key, Object... members) {
        return redisTemplate.opsForZSet().remove(key, members);
    }

    /**
     * 获取有序集合大小
     */
    public Long zcard(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * 获取有序集合成员的分数
     */
    public Double zscore(String key, Object member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    /**
     * 获取有序集合指定范围成员（按分数升序）
     */
    public Set<Object> zrange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取有序集合指定范围成员（按分数降序）
     */
    public Set<Object> zrevrange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取有序集合指定分数范围内的成员
     */
    public Set<Object> zrangebyscore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * 获取有序集合成员排名（从小到大）
     */
    public Long zrank(String key, Object member) {
        return redisTemplate.opsForZSet().rank(key, member);
    }

    /**
     * 获取有序集合成员排名（从大到小）
     */
    public Long zrevrank(String key, Object member) {
        return redisTemplate.opsForZSet().reverseRank(key, member);
    }

    /**
     * 统计有序集合指定分数范围内的成员数
     */
    public Long zcount(String key, double min, double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    /**
     * 对有序集合成员分数进行递增
     */
    public Double zincrby(String key, Object member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

}

