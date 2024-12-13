package org.example.ratelimiter.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 常量
 * 一些限流器默认参数（目前来看作用不大）
 *
 * @author Percy
 * @date 2024/12/10
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
    /**
     * PermitLimiter
     */
    public static final long PERMITS_PER_SECOND = 100L;
    public static final long MAX_PERMITS = 100L;
}
