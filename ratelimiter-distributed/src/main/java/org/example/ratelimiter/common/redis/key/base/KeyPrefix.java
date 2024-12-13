package org.example.ratelimiter.common.redis.key.base;

public interface KeyPrefix {
    int expireSeconds();

    String getPrefix();
}
