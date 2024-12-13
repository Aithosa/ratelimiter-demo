package org.example.ratelimiter.common.redis.service;

import org.example.ratelimiter.common.redis.key.base.KeyPrefix;
import org.example.ratelimiter.utils.BeanUtils;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 基本方法以及自带的限流器和锁
 *
 * @author Percy
 * @date 2024/12/10
 */
@Service
public class RedissonService {
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取Redis锁
     *
     * @param key 锁的key
     * @return 锁对象
     */
    public RLock getRLock(String key) {
        return redissonClient.getLock(key);
    }

    /**
     * 获取Redis读写锁
     *
     * @param key 锁的key
     * @return 读写锁对象
     */
    public RReadWriteLock getRWLock(String key) {
        return redissonClient.getReadWriteLock(key);
    }

    /**
     * 获取限流器
     *
     * @param key 限流器名称
     * @return 限流器实例
     */
    public RRateLimiter getRateLimiter(String key) {
        return redissonClient.getRateLimiter(key);
    }

    public <T> boolean exists(KeyPrefix prefix, String key) {
        String realKey = prefix.getPrefix() + key;
        // 使用 Redisson 的 RBucket 检查键是否存在
        return redissonClient.getBucket(realKey).isExists();
    }

    public boolean delete(KeyPrefix prefix, String key) {
        String realKey = prefix.getPrefix() + key;
        return redissonClient.getBucket(realKey).delete();
    }

    public <T> T get(KeyPrefix prefix, String key, Class<T> clazz) {
        String realKey = prefix.getPrefix() + key;
        String str = (String) redissonClient.getBucket(realKey).get();
        return BeanUtils.stringToBean(str, clazz);
    }

    public <T> boolean set(KeyPrefix prefix, String key, T value) {
        String realKey = prefix.getPrefix() + key;
        String str = BeanUtils.beanToString(value);
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 获取过期时间
        int seconds = prefix.expireSeconds();
        if (seconds <= 0) {
            // 永久保存键
            redissonClient.getBucket(realKey).set(str);
        } else {
            // 保存键并设置过期时间
            redissonClient.getBucket(realKey).set(str, seconds, TimeUnit.SECONDS);
        }
        return true;
    }

    public <T> boolean setwe(KeyPrefix prefix, String key, T value, int expireSeconds) {
        String realKey = prefix.getPrefix() + key;
        String str = BeanUtils.beanToString(value);
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 判断是否需要设置过期时间
        if (expireSeconds <= 0) {
            // 永不过期（等价于 Jedis 的 set）
            redissonClient.getBucket(realKey).set(str);
        } else {
            // 设置过期时间
            redissonClient.getBucket(realKey).set(str, expireSeconds, TimeUnit.SECONDS);
        }
        return true;
    }

    public <T> T hget(KeyPrefix prefix, String key, String field, Class<T> clazz) {
        String realKey = prefix.getPrefix() + key;
        String str = (String) redissonClient.getMap(realKey).get(field);
        return BeanUtils.stringToBean(str, clazz);
    }

    public <T> boolean hset(KeyPrefix prefix, String key, String field, T value) {
        String realKey = prefix.getPrefix() + key;
        String str = BeanUtils.beanToString(value);
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 获取 RMap 对象，插入字段和值
        var rMap = redissonClient.getMap(realKey);
        rMap.put(field, str);

        // 设置过期时间
        int seconds = prefix.expireSeconds();
        if (seconds > 0) {
            rMap.expire(seconds, TimeUnit.SECONDS);
        }

        return true;
    }

    public <T> Long incr(KeyPrefix prefix, String key) {
        String realKey = prefix.getPrefix() + key;
        return redissonClient.getAtomicLong(realKey).incrementAndGet();
    }

    public <T> Long decr(KeyPrefix prefix, String key) {
        String realKey = prefix.getPrefix() + key;
        return redissonClient.getAtomicLong(realKey).decrementAndGet();
    }

    public <T> boolean hmcset(KeyPrefix prefix, String key, T value) {
        String realKey = prefix.getPrefix() + key;

        Map<String, String> map = BeanUtils.beanToMap(value);
        if (map == null || map.isEmpty()) {
            return false;
        }

        // 获取 RMap，向 Redis 中插入数据
        RMap<String, String> redissonMap = redissonClient.getMap(realKey);
        redissonMap.putAll(map);

        // 设置过期时间
        int seconds = prefix.expireSeconds();
        if (seconds > 0) {
            // 设置全局 TTL
            redissonMap.expire(seconds, TimeUnit.SECONDS);
        }

        return true;
    }

    public <T> T hmcget(KeyPrefix prefix, String key, Class<T> clazz) {
        String realKey = prefix.getPrefix() + key;
        Map<String, String> map = redissonClient.<String, String>getMap(realKey).readAllMap();
        return BeanUtils.mapToBean(map, clazz);
    }
}
