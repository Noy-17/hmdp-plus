package org.javaup;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 用户微服务启动入口 (端口 8082)
 * 管理 tb_user / tb_user_info / tb_user_phone 三张分片表，提供注册登录和信息查询。
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("org.javaup.mapper")
@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
