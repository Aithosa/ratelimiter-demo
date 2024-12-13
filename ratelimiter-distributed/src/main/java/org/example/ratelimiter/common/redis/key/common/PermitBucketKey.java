package org.example.ratelimiter.common.redis.key.common;


import org.example.ratelimiter.common.redis.key.base.BasePrefix;

/**
 * 限流器实例信息
 *
 * @author Percy
 * @date 2024/12/13
 */
public class PermitBucketKey extends BasePrefix {
    private PermitBucketKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    public static final PermitBucketKey permitBucket = new PermitBucketKey(0, "RL");
}
