package org.example.ratelimiter.limiter.ratelimiter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.ratelimiter.common.constant.SwitchEnum;
import org.example.ratelimiter.common.redis.key.common.PermitBucketKey;
import org.example.ratelimiter.common.redis.service.RedissonService;
import org.example.ratelimiter.limiter.base.Limiter;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 分布式令牌桶限流器，以Guava令牌桶为基础
 *
 * @author Percy
 * @date 2024/12/13
 */
@Slf4j
@Getter(AccessLevel.PRIVATE)
public class RateLimiter implements Limiter {
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
     * 超时时间 - 由缓存队列比例计算
     * TODO 这个设计考虑到令牌在动态生成和消耗吗？
     */
    private final long timeoutMicros;

    /**
     * 分布式互斥锁
     */
    private final RLock lock;

    /**
     * 用于对Redis进行读取和查找操作
     */
    private final RedissonService redisService;

    /**
     * 构造函数
     *
     * @param config 配置数据
     */
    public RateLimiter(RateLimiterConfig config) {
        this.name = config.getName();
        // 不设置则使用默认值
        this.permitsPerSecond = (config.getPermitsPerSecond() == 0L) ? 1000L : config.getPermitsPerSecond();
        this.maxPermits = config.getMaxPermits();
        // 令牌生成速率
        long intervalMicros = TimeUnit.SECONDS.toMicros(1) / permitsPerSecond;
        // 缓存比例*每秒生成的令牌数=缓存队列长度；缓存队列长度*单个令牌生成速率=该缓存队列生成满的时间
        this.timeoutMicros = (long) (config.getCache() * config.getPermitsPerSecond() * intervalMicros);
        this.lock = config.getLock();
        this.redisService = config.getRedisService();
        log.info("Creat rateLimiter: {}, maxPermits: {}, permitsPerSecond: {}, intervalMicros:{}, timeoutMicros: {}",
                name, maxPermits, permitsPerSecond, intervalMicros, timeoutMicros);
    }

    /**
     * 返回每秒生成令牌数量
     *
     * @return 每秒生成令牌数量
     */
    public long getRate() {
        return this.permitsPerSecond;
    }

    /**
     * 获取限流器实例名称
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * 尝试获取锁
     * NOTE: 如果 lock() 方法捕获到 InterruptedException 后不恢复中断状态，
     * 外层的 while 循环将无法正常退出，因为 Thread.interrupted() 永远返回 false。
     *
     * @return 获取成功返回 true
     */
    private boolean lock() {
        try {
            // 等待 100 秒，获得锁 100 秒后自动解锁
            boolean acquired = lock.tryLock(100, 100, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire lock after waiting for 100 seconds");
            }
            return acquired;
        } catch (InterruptedException e) {
            // 恢复中断状态
            Thread.currentThread().interrupt();
            log.error("Lock acquisition was interrupted", e);
            return false;
        }
    }

    /**
     * 释放锁
     */
    private void unlock() {
        lock.unlock();  // 直接释放锁，如果没有持有锁会抛出 IllegalMonitorStateException
    }

//    /**
//     * 生成并存储默认令牌桶信息到 Redis 中
//     * TODO 必须在 lock 内调用（避免外部调用，内部也要在lock内）
//     * TODO 又判断又存又取会消耗一些时间（有的方法调用不需要返回，可以再创建一个不取的方法），并且需要加锁吗
//     *
//     * @return 限流器状态
//     */
//    public PermitBucket putDefaultBucket() {
//        if (!redisService.exists(PermitBucketKey.permitBucket, this.name)) {
//            long intervalMicros = TimeUnit.SECONDS.toMicros(1) / permitsPerSecond;
//            long nextFreeTicketMicros = MILLISECONDS.toMicros(System.currentTimeMillis());
//            PermitBucket permitBucket = new PermitBucket(name, maxPermits, 1, intervalMicros, nextFreeTicketMicros);
//            // 存入缓存，设置有效时间
//            redisService.setwe(PermitBucketKey.permitBucket, this.name, permitBucket, PermitBucketKey.permitBucket.expireSeconds());
//        }
//
//        // 某方面可以防止存入失败
//        return redisService.get(PermitBucketKey.permitBucket, this.name, PermitBucket.class);
//    }

    /**
     * 从Redis获取令牌桶的状态信息
     * 不存在则会根据当前限流器实例信息重新创建
     * NOTE: 必须在 lock 内调用（避免外部调用，内部也要在lock内）
     * TODO 又判断又存又取会消耗一些时间（有的方法调用不需要返回，可以再创建一个不取的方法），并且需要加锁吗
     *
     * @return 限流器状态
     */
    public PermitBucket getOrCreateBucket() {
        if (!redisService.exists(PermitBucketKey.permitBucket, this.name)) {
            return putDefaultBucket();
        }

        return redisService.get(PermitBucketKey.permitBucket, this.name, PermitBucket.class);
    }

    /**
     * 创建限流器状态信息并存入Redis
     * 用于初始化及重建
     *
     * @return 限流器状态
     */
    public PermitBucket putDefaultBucket() {
        long intervalMicros = TimeUnit.SECONDS.toMicros(1) / permitsPerSecond;
        long nextFreeTicketMicros = MILLISECONDS.toMicros(System.currentTimeMillis());
        PermitBucket permitBucket = new PermitBucket(name, maxPermits, 1, intervalMicros, nextFreeTicketMicros);
        // 存入缓存，设置有效时间
        setBucket(permitBucket);

        return permitBucket;
    }

    /**
     * 获取令牌桶, 不刷新，用于acquire
     *
     * @return 缓存中的令牌桶或者默认的令牌桶
     */
    public PermitBucket getBucket() {
        // 从缓存中获取桶
        PermitBucket permitBucket = redisService.get(PermitBucketKey.permitBucket, this.name, PermitBucket.class);
        // 如果缓存中没有，进入 putDefaultBucket 中初始化
        if (permitBucket == null) {
            return getOrCreateBucket();
        }

        return permitBucket;
    }

    /**
     * 获取令牌桶, 并刷新令牌桶状态, 用于仅查询
     *
     * @return 缓存中的令牌桶或者默认的令牌桶
     */
    public PermitBucket getBucketAndSync() {
        PermitBucket permitBucket = redisService.get(PermitBucketKey.permitBucket, this.name, PermitBucket.class);
        if (permitBucket == null) {
            // 如果缓存中没有，进入 putDefaultBucket 中初始化
            return getOrCreateBucket();
        }

        // TODO 刷新桶状态信息但是并不回写 这个方法有问题，这行代码没意义
        permitBucket.reSync();

        return redisService.get(PermitBucketKey.permitBucket, this.name, PermitBucket.class);
    }

    /**
     * 按给定配置重新设置桶的状态信息
     *
     * @param permitBucket 新的令牌桶状态信息
     */
    private void setBucket(PermitBucket permitBucket) {
        redisService.setwe(PermitBucketKey.permitBucket, this.name, permitBucket, PermitBucketKey.permitBucket.expireSeconds());
    }

    /**
     * 根据请求的令牌数量，计算需要等待的时间（微秒）并保留资源（令牌桶中对应的令牌）
     *
     * @param permits 请求的令牌数量
     * @return 成功获取令牌所需的等待时间，单位为微秒
     */
    private long reserve(int permits) {
        checkPermits(permits);
        while (true) {
            if (lock()) {
                try {
                    // 执行资源预定逻辑并获取等待时间
                    return reserveAndGetWaitLength(permits, MILLISECONDS.toMicros(System.currentTimeMillis()));
                } finally {
                    unlock();
                }
            } else {
                log.info("lock failed, try another");
            }
        }
    }

    /**
     * 为请求的令牌数量保留相应的资源，并计算需要等待的时间
     *
     * @param permits 请求的令牌数
     * @param nowMicros 当前时间，单位为微秒
     * @return 需要等待的时间，单位为微秒
     */
    private long reserveAndGetWaitLength(long permits, long nowMicros) {
        long momentAvailable = reserveEarliestAvailable(permits, nowMicros);
        return max(momentAvailable - nowMicros, 0);
    }

    /**
     * 为本次请求的令牌数量预定资源，并计算最早可以满足请求的时间
     *
     * @param requiredPermits 本次请求的令牌数量
     * @param nowMicros 当前时间戳，单位为微秒
     * @return 供令牌可用的最早时间点，单位为微秒
     */
    private long reserveEarliestAvailable(long requiredPermits, long nowMicros) {
        PermitBucket bucket = getBucket();
        // 计算生成的令牌数信息
        bucket.reSync(nowMicros);

        // 结合这次请求，当前总共能提供出去的令牌数
        long storedPermitsToSpend = min(requiredPermits, bucket.getStoredPermits());
        // 这次请求还欠的令牌数
        long freshPermits = requiredPermits - storedPermitsToSpend;
        // 生成还欠的令牌数需要花的时间
        long waitMicros = freshPermits * bucket.getIntervalMicros();

        // 更新令牌桶下次可以发放令牌的时间戳
        bucket.setNextFreeTicketMicros(Limiter.saturatedAdd(bucket.getNextFreeTicketMicros(), waitMicros));
        long returnValue = bucket.getNextFreeTicketMicros();
        // 这里不为负，最多为0，后面会休眠负令牌清零的时间，等待令牌恢复
        // 扣掉本次请求满足时能提供出去的所有令牌
        bucket.setStoredPermits(bucket.getStoredPermits() - storedPermitsToSpend);
        setBucket(bucket);

        return returnValue;
    }

    private boolean canAcquire(long permits, long nowMicros, long timeoutMicros) {
        return queryEarliestAvailable(permits, nowMicros) - timeoutMicros <= nowMicros;
    }

    private long queryEarliestAvailable(long permits, long nowMicros) {
        return estimateEarliestAvailable(permits, nowMicros);
    }

    /**
     * 估算满足请求的令牌数量后，令牌桶下次可以发放令牌的时间戳
     *
     * @param requiredPermits 本次请求的令牌数量
     * @param nowMicros 当前时间戳，单位为微秒
     * @return 供令牌可用的最早时间点，单位为微秒
     */
    private long estimateEarliestAvailable(long requiredPermits, long nowMicros) {
        PermitBucket bucket = getBucket();
        bucket.reSync(nowMicros);

        // 结合这次请求，当前总共能提供出去的令牌数
        long storedPermitsToSpend = min(requiredPermits, bucket.getStoredPermits());
        // 这次请求还欠的令牌数
        long freshPermits = requiredPermits - storedPermitsToSpend;
        // 生成还欠的令牌数需要花的时间
        long waitMicros = freshPermits * bucket.getIntervalMicros();

        return Limiter.saturatedAdd(bucket.getNextFreeTicketMicros(), waitMicros);
    }

    /**
     * 获取一个令牌
     *
     * @return 实际等待时间，单位为秒
     */
    @Override
    public double acquire() {
        return acquire(1);
    }

    /**
     * 获取指定数量令牌
     *
     * @return 实际等待时间，单位为秒
     */
    public double acquire(int permits) {
        // 计算等待时间（并做了实际扣减）
        long microsToWait = reserve(permits);
        Limiter.sleepMicrosUninterruptibly(microsToWait);
        return 1.0 * microsToWait / SECONDS.toMicros(1L);
    }

    /**
     * 尝试获取1个令牌
     * - 考虑开关状态
     * - 使用预设允许的最大等待时间和单位，单位为微秒
     *
     * @param switchConf 开关状态
     * @return 获取结果
     */
    public boolean tryAcquire(String switchConf) {
        if (SwitchEnum.OFF.getCode().equals(switchConf)) {
            return true;
        }

        return tryAcquire(1, timeoutMicros, TimeUnit.MICROSECONDS);
    }

    /**
     * 获取1个令牌, 指定预设允许的最大等待时间和单位（以微秒为单位）
     *
     * @return 获取结果
     */
    public boolean tryAcquire() {
        return tryAcquire(1, timeoutMicros, TimeUnit.MICROSECONDS);
    }

    /**
     * 获取1个令牌, 指定允许的最大等待时间和单位（以毫秒为单位）
     *
     * @param timeoutMillis 获取这些令牌允许的最大等待时间（毫秒）
     * @return 获取结果
     */
    public boolean tryAcquire(long timeoutMillis) {
        return tryAcquire(1, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取1个令牌, 指定允许的最大等待时间和单位
     *
     * @param timeout 获取这些令牌允许的最大等待时间
     * @param unit timeout的时间单位
     * @return 获取结果
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) {
        return tryAcquire(1, timeout, unit);
    }

    /**
     * 尝试根据给定的条件（请求的令牌数量、超时时间）获取指定数量的令牌
     * 获取成功或超时才返回
     *
     * @param permits 获取的令牌数
     * @param timeout 获取这些令牌允许的最大等待时间
     * @param unit timeout的时间单位
     * @return 获取结果
     */
    public boolean tryAcquire(long permits, long timeout, TimeUnit unit) {
        checkPermits(permits);
        long timeoutMicros = max(unit.toMicros(timeout), 0);
        long waitMicros = 0L;

        while (true) {
            if (lock()) {
                try {
                    long nowMicros = MILLISECONDS.toMicros(System.currentTimeMillis());
                    // 判断是否可以在剩余的超时时间内成功获取到指定数量的令牌（不做实际扣减）
                    if (!canAcquire(permits, nowMicros, timeoutMicros)) {
                        return false;
                    } else {
                        // 成功获取令牌后保留资源，并记录等待时长
                        waitMicros = reserveAndGetWaitLength(permits, nowMicros);
                    }
                } finally {
                    unlock();
                }

                Limiter.sleepMicrosUninterruptibly(waitMicros);
                return true;
            }
        }
    }

    /**
     * 添加指定数量令牌, 不能超过桶的大小
     *
     * @param permits 要添加的令牌数
     */
    public void addPermits(long permits) {
        checkPermits(permits);

        while (true) {
            if (lock()) {
                try {
                    PermitBucket bucket = getBucket();
                    bucket.reSync(MILLISECONDS.toMicros(System.currentTimeMillis()));
                    long newPermits = calculateAddPermits(bucket, permits);
                    // TODO 这里是直接给令牌，不需要要等待，是不是不应该调用这个方法
                    long newNextFreeTicketMicros = calculateNextFreeTicketMicros(bucket, newPermits);
                    bucket.setStoredPermits(newPermits);
                    bucket.setNextFreeTicketMicros(newNextFreeTicketMicros);
                    setBucket(bucket);
                    return;
                } finally {
                    unlock();
                }
            }
        }
    }

    /**
     * 计算添加给定数量令牌后桶里的令牌数
     *
     * @param bucket     桶
     * @param addPermits 添加的令牌数
     * @return 更新后桶里的令牌数
     */
    private long calculateAddPermits(PermitBucket bucket, long addPermits) {
        long newPermits = bucket.getStoredPermits() + addPermits;

        if (newPermits > bucket.getMaxPermits()) {
            newPermits = bucket.getMaxPermits();
        }

        return newPermits;
    }

    /**
     * 计算生成给定数量令牌后，系统可以安全地尝试获取新令牌的时间
     *
     * @param bucket     桶信息
     * @param addPermits 添加的令牌数
     * @return 系统下次可以安全地尝试获取新令牌的时间
     */
    private long calculateNextFreeTicketMicros(PermitBucket bucket, long addPermits) {
        long addTimeMicros = bucket.getIntervalMicros() * addPermits;
        long nowMicros = MILLISECONDS.toMicros(System.currentTimeMillis());
        // 新下次请求的可用时间（也就是不欠令牌的时间/安全获取的时间）
        // 假设从现在开始生成新令牌，下次可以可用时点的时间戳
        long newNextFreeTicketMicros = nowMicros + addTimeMicros;
        // 如果当前计算出的下一次可用时间晚于桶的现有可用时间，
        // 说明令牌桶已经没有欠的令牌，可以立即重新计时，从当前时间开始
        if (newNextFreeTicketMicros > bucket.getNextFreeTicketMicros()) {
            return nowMicros;
        }

        // 等待这段时间后，才可以进行下一次请求
        return newNextFreeTicketMicros;
    }

//    /**
//     * 当前是否可以获取到令牌，如果获取不到，至少需要等多久
//     *
//     * @param permits 请求的令牌数
//     * @return 等待时间，单位是纳秒。为 0 表示可以马上获取
//     */
//    private long canAcquire(long permits) {
//        // 读取redis中的令牌数据, 同步令牌状态, 写回redis
//        PermitBucket bucket = getBucket();
//        long now = System.nanoTime();
//        bucket.reSync(now);
//        setBucket(bucket);
//
//        if (permits <= bucket.getStoredPermits()) {
//            return 0L;
//        } else {
//            return (permits - bucket.getStoredPermits()) * bucket.getIntervalMicros();
//        }
//    }

    private boolean acquireInTime(long startNanos, long waitNanos, long timeoutNanos) {
//        return waitNanos - timeoutNanos <= startNanos;
        return waitNanos <= timeoutNanos;
    }

    /**
     * 校验 permits 值是否合法
     *
     * @param permits permits 值
     */
    private void checkPermits(long permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("Request/Put permits " + permits + " must be positive");
        }
    }
}
