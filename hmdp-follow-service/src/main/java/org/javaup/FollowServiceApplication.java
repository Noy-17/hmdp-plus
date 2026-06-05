package org.javaup;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 关注微服务启动入口 (端口 8085)
 * 负责用户关注/取关/共同关注，通过 UserBridge 跨域读取用户数据。
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("org.javaup.mapper")
@SpringBootApplication
public class FollowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FollowServiceApplication.class, args);
    }
}
