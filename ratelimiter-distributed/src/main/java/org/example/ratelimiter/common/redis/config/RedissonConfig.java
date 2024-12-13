package org.example.ratelimiter.common.redis.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Redis配置类
 *
 * @author Percy
 * @date 2024/12/13
 */
@Data
@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private String port;

    @Value("${spring.data.redis.database}")
    private int database;

    @Value("${spring.data.redis.password}")
    private String password;
}
