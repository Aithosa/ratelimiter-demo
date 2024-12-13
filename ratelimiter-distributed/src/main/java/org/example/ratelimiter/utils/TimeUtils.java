package org.example.ratelimiter.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 时间工具类
 *
 * @author Percy
 * @date 2024/12/13
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeUtils {
    /**
     * 将毫秒转化为秒，保留3位小数
     *
     * @param millis 毫秒数
     * @return 秒（double类型）
     */
    public static double millisToSeconds(long millis) {
        return millis / 1000.0;
    }
}
