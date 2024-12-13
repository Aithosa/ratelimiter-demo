package org.example.ratelimiter.model;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 接口调用限制信息配置管理
 *
 * @author Percy
 * @date 2024/12/13
 */
@Mapper
public interface TAirRatelimitConfMapper {
    List<TAirRatelimitConf> getAirRatelimitConf();
}
