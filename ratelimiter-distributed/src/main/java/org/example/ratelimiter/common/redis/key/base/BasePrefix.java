package org.example.ratelimiter.common.redis.key.base;

import lombok.AllArgsConstructor;

/**
 * 限流器实例信息
 *
 * @author Percy
 * @date 2024/12/13
 */
@AllArgsConstructor
public abstract class BasePrefix implements KeyPrefix {
    private final int expireSeconds;

    private final String prefix;

    /**
     * 默认0代表永不过期
     */
    protected BasePrefix(String prefix) {
        this.expireSeconds = 0;
        this.prefix = prefix;
    }

    @Override
    public int expireSeconds() {
        return expireSeconds;
    }

    @Override
    public String getPrefix() {
        String className = getClass().getSimpleName();
        return className + ":" + prefix + ":";
    }
}
