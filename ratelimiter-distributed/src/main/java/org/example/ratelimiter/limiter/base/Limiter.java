package org.example.ratelimiter.limiter.base;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 限流器接口
 *
 * @author Percy
 * @date 2024/12/13
 */
public interface Limiter {
    double acquire();

    /**
     * 休眠指定毫秒数
     *
     * @param micros 要休眠的毫秒数
     */
    public static void sleepMicrosUninterruptibly(long micros) {
        if (micros > 0) {
            sleepUninterruptibly(micros, MICROSECONDS);
        }
    }

    /**
     * 让当前线程休眠指定的时间，即使线程被中断，也会继续完成剩余的休眠时间。
     * 最终，在休眠结束后，如果线程被中断过，会在 `finally` 块中重置中断状态。
     *
     * @param sleepFor 需要休眠的时间
     * @param unit 时间的单位
     */
    public static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
        boolean interrupted = false;
        try {
            long remainingNanos = unit.toNanos(sleepFor);
            long end = System.nanoTime() + remainingNanos;
            while (true) {
                try {
                    // TimeUnit.sleep() treats negative timeouts just like zero.
                    NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            // 不吞掉中断信号，让调用方知道休眠期间被中断过，从而可以正确处理或传播中断
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 处理饱和加法
     * - 该方法实现了一个安全的加法运算。
     * - 加法若未发生溢出，则直接返回结果（`naiveSum`）。
     * - 若发生溢出，算法通过符号判断返回：
     *     - 溢出为正方向时，返回 `Long.MAX_VALUE`。
     *     - 溢出为负方向时，返回 `Long.MIN_VALUE`
     * <p>
     * 暂时使用Guava的@beta(是否有替代函数)
     */
    public static long saturatedAdd(long a, long b) {
        long naiveSum = a + b;
        if ((a ^ b) < 0 | (a ^ naiveSum) >= 0) {
            // If a and b have different signs or a has the same sign as the result then there was no
            // overflow, return.
            return naiveSum;
        }
        // we did over/under flow, if the sign is negative we should return MAX otherwise MIN
        return Long.MAX_VALUE + ((naiveSum >>> (Long.SIZE - 1)) ^ 1);
    }
}
