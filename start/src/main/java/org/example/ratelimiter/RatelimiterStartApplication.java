package org.example.ratelimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
`@MapperScan` 的作用**：
它是 MyBatis 提供的注解，用于批量扫描指定包路径下的 Mapper 接口，将它们注册为 Spring 容器中的 Bean
如果你所有的 Mapper 接口都手动加上了 `@Mapper` 注解，也可以不用添加 `@MapperScan`
 */
//@MapperScan("org.example.ratelimiter.model")
@SpringBootApplication
public class RatelimiterStartApplication {

    public static void main(String[] args) {
        SpringApplication.run(RatelimiterStartApplication.class, args);
    }

}
