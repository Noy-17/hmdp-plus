package org.javaup;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.javaup.feign.config.FeignAuthConfig;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 博客微服务启动入口 (端口 8084)
 * 负责探店笔记发布/点赞/Feed 流滚动分页，通过 UserBridge + FollowBridge 跨域读取用户和关注数据。
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "org.javaup.feign", defaultConfiguration = FeignAuthConfig.class)
@MapperScan("org.javaup.mapper")
@SpringBootApplication
public class BlogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogServiceApplication.class, args);
    }
}
