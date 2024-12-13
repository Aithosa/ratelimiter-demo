package org.example.ratelimiter.limiter.base;

public interface Factory {
    /**
     * 销毁
     *
     * @param obj the obj
     */
    void destroy(Object obj);
}
