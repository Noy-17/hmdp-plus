package org.javaup;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 商铺微服务启动入口 (端口 8081)
 * 负责商铺缓存查询、GEO 地理位置搜索、布隆过滤器防穿透，零跨域依赖。
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("org.javaup.mapper")
@SpringBootApplication
public class ShopServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopServiceApplication.class, args);
    }
}
