package org.example.ratelimiter.limiter.ratelimiter;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 令牌桶状态信息,
 * 存放在Redis中的结构
 * 可以根据名称给每个接口做定义，直接在配置表中保存名称，用配置表里接口的名称
 *
 * @author Percy
 * @date 2024/12/13
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermitBucket {
    /**
     * 唯一标识
     */
    private String name;

    /**
     * 最大存储令牌数
     */
    private long maxPermits;

    /**
     * 当前存储令牌数
     */
    private long storedPermits;

    /**
     * 令牌生成速度
     * 每两次添加令牌之间的时间间隔（逐个添加令牌），单位为纳秒
     */
    private long intervalMicros;

    /**
     * 下一次可以响应请求的时间（因为限流器有预支令牌的情况，所以只有不欠的时候才可以响应后续请求）
     */
    private long nextFreeTicketMicros;

    /**
     * 更新当前持有的令牌数, 同步令牌桶的状态
     * 根据当前时间和上一次时间戳的间隔，更新令牌桶中当前令牌数。
     * 若当前时间晚于 nextFreeTicketMicros，则计算该段时间内可以生成多少令牌，将生成的令牌加入令牌桶中并更新数据
     *
     * @param nowMicros 当前时间
     */
    public void reSync(long nowMicros) {
        // 当前时间大于下次更新令牌的时间，才会执行更新，否则不变
        if (nowMicros > nextFreeTicketMicros) {
            // long newStoredPermits = Math.min(maxPermits, storedPermits + (now - lastUpdateTime) / intervalNanos);
            // 这里令牌数量其实会不能整除，这种情况下更新nextFreeTicketMicros会损失部分时间精度
            long newPermits = (nowMicros - nextFreeTicketMicros) / intervalMicros;
            storedPermits = min(maxPermits, storedPermits + newPermits);
            // 如果时间还不够生成新的令牌，不需要更新nextFreeTicketMicros
            if (newPermits > 0L) {
                nextFreeTicketMicros = nowMicros;
            }
        }
    }

    /**
     * 默认使用当前时间更新桶状态
     */
    public void reSync() {
        reSync(MILLISECONDS.toMicros(System.currentTimeMillis()));
    }

    /**
     * 没有的话RedisService的方法执行有可能会报错，或者换成Jackson
     * NOTE: 为了统一引用换成了fastjson
     */
    public PermitBucket(String json) {
        PermitBucket param = JSON.parseObject(json, PermitBucket.class);
        this.name = param.getName();
        this.maxPermits = param.getMaxPermits();
        this.storedPermits = param.getStoredPermits();
        this.intervalMicros = param.getIntervalMicros();
        this.nextFreeTicketMicros = param.getNextFreeTicketMicros();
    }
}
