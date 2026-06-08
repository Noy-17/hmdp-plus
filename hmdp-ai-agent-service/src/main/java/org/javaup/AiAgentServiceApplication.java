package org.javaup;

import org.javaup.feign.config.FeignAuthConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "org.javaup.feign", defaultConfiguration = FeignAuthConfig.class)
@MapperScan("org.javaup.mapper")
@SpringBootApplication
public class AiAgentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAgentServiceApplication.class, args);
    }
}
