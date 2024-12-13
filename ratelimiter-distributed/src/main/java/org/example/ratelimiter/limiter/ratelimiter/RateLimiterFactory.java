package org.example.ratelimiter.limiter.ratelimiter;

import org.example.ratelimiter.limiter.base.Factory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器工厂类
 * 管理所有实例
 *
 * @author Percy
 * @date 2024/12/13
 */
@Service
public class RateLimiterFactory implements Factory {
    /**
     * 双向存储检索
     */
    private static final Map<String, RateLimiter> RATELIMITERS = new ConcurrentHashMap<>();
    private static final Map<RateLimiter, String> RATELIMITERS_NAME = new ConcurrentHashMap<>();

    /**
     * 按配置名称获取限流器，不存在则用配置创建
     *
     * @param config 限流器配置
     * @return 取出或创建的限流器
     */
    public RateLimiter getPermitLimiter(RateLimiterConfig config) {
        RateLimiter rateLimiter = RATELIMITERS.get(config.getName());
        if (rateLimiter == null) {
            rateLimiter = new RateLimiter(config);
            String name = rateLimiter.getName();
            RATELIMITERS.putIfAbsent(name, rateLimiter);
            RATELIMITERS_NAME.putIfAbsent(rateLimiter, name);

            // TODO 为什么又获取一遍，直接返回会不会更好
            rateLimiter = RATELIMITERS.get(name);
            // TODO 这个方法调用需要加锁吗？
            // 配置存到redis中
            rateLimiter.putDefaultBucket();
        }

        return rateLimiter;
    }

    /**
     * 注销限流器
     *
     * @param obj the obj
     */
    @Override
    public void destroy(Object obj) {
        if (obj instanceof RateLimiter) {
            String name = RATELIMITERS_NAME.remove(obj);
            RATELIMITERS.remove(name);
        }
    }
}
