package org.example.ratelimiter.common.redis.config.factory;

import org.example.ratelimiter.common.redis.config.RedissonConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Redis创建工厂
 *
 * @author Percy
 * @date 2024/12/13
 */
@Service
public class RedissonFactory {
    @Autowired
    RedissonConfig redissonConfig;

    @Bean
    public RedissonClient getRedisson() {
        String host = redissonConfig.getHost();
        String port = redissonConfig.getPort();
        int database = redissonConfig.getDatabase();
        String password = redissonConfig.getPassword();
        // Redisson密码如果传空字符串会报错无法启动
        if (StringUtils.isEmpty(password)) {
            password = null;
        }

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setPassword(password);
        //添加主从配置
//        config.useMasterSlaveServers()
//                .setMasterAddress("")
//                .setPassword("")
//                .addSlaveAddress(new String[]{"", ""});
        return Redisson.create(config);
    }
}
