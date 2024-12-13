package org.example.ratelimiter.limiter.ratelimiter;

import lombok.Getter;
import lombok.ToString;
import org.example.ratelimiter.common.constant.Constants;
import org.example.ratelimiter.common.redis.service.RedissonService;
import org.redisson.api.RLock;

/**
 * 限流器完整配置
 *
 * @author Percy
 * @date 2024/12/13
 */
@Getter
@ToString
public class RateLimiterConfig {
    /**
     * 唯一标识
     */
    private final String name;

    /**
     * 每秒存入的令牌数
     */
    private final long permitsPerSecond;

    /**
     * 最大存储令牌数
     */
    private final long maxPermits;

    /**
     * 缓存比例
     */
    private final float cache;

    /**
     * 分布式互斥锁
     */
    private final RLock lock;

    /**
     * 用于对Redis进行读取和查找操作
     */
    private final RedissonService redisService;

    /**
     * 限流参数取默认值，没有缓存
     *
     * @param name 限流器名称
     * @param lock 分布式互斥锁
     * @param redisService redis服务
     */
    public RateLimiterConfig(String name, RLock lock, RedissonService redisService) {
        this(name, Constants.PERMITS_PER_SECOND, Constants.MAX_PERMITS, 0F, lock, redisService);
    }

    /**
     * 桶的大小一秒钟就可以填满，没有缓存
     *
     * @param name 限流器名称
     * @param permitsPerSecond 每秒存入的令牌数
     * @param lock 分布式互斥锁
     * @param redisService redis服务
     */
    public RateLimiterConfig(String name, long permitsPerSecond, RLock lock, RedissonService redisService) {
        this(name, permitsPerSecond, permitsPerSecond, 0F, lock, redisService);
    }

    /**
     * 桶的大小一秒钟就可以填满，指定缓存大小
     *
     * @param name 限流器名称
     * @param permitsPerSecond 每秒存入的令牌数
     * @param cache 缓存比例
     * @param lock 分布式互斥锁
     * @param redisService redis服务
     */
    public RateLimiterConfig(String name, long permitsPerSecond, float cache, RLock lock, RedissonService redisService) {
        this(name, permitsPerSecond, permitsPerSecond, cache, lock, redisService);
    }

    public RateLimiterConfig(String name, long permitsPerSecond, long maxPermits, float cache, RLock lock, RedissonService redisService) {
        this.name = name;
        this.permitsPerSecond = permitsPerSecond;
        this.maxPermits = maxPermits;
        this.cache = cache;
        this.lock = lock;
        this.redisService = redisService;
    }
}
