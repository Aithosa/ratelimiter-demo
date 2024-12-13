package org.example.ratelimiter.model;

import lombok.Data;

/**
 * 接口调用限制信息配置
 * 用于给需要限流的接口初始化限流器
 *
 * @author Percy
 * @date 2024/12/13
 */
@Data
public class TAirRatelimitConf {
    /**
     * 接口渠道归属
     */
    private String channelType;

    /**
     * 接口名
     */
    private String interfaceNo;

    /**
     * 接口url
     */
    private String interfaceUrl;

    /**
     * 调用限制（每秒调用次数）
     */
    private Integer rateLimit;

    /**
     * 允许缓存的请求数量比例
     */
    private Float cache;

    /**
     * 限流开关
     * NOTE:放在这里改了数据库也无法生效，除非后台有接口在修改值后清除对应实例或者更改开关配置
     */
    private boolean status;
}
