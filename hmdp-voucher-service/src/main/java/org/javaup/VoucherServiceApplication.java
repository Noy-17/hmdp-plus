package org.javaup;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.javaup.feign.config.FeignAuthConfig;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

/**
 * 秒杀微服务启动入口 (端口 8083)。
 *
 * 包含秒杀全链路：令牌前置授权 → 令牌桶限流 → Lua 原子扣减 → RabbitMQ 消息投递 → 订单创建 → 对账。
 * 涵盖 Voucher + SeckillVoucher + VoucherOrder 三域，因双向循环依赖无法拆分。
 * 是 5 个服务中唯一启用 {@code @EnableRabbit} 的服务。
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "org.javaup.feign", defaultConfiguration = FeignAuthConfig.class)
@MapperScan("org.javaup.mapper")
@SpringBootApplication
@EnableRabbit
public class VoucherServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VoucherServiceApplication.class, args);
    }
}
