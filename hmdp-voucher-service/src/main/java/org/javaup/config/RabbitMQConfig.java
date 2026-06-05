package org.javaup.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.javaup.constant.Constant.SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC;
import static org.javaup.constant.Constant.SECKILL_VOUCHER_TOPIC;

@Configuration
public class RabbitMQConfig {

    @Value("${prefix.distinction.name:hmdp}")
    private String prefix;

    private String x(String topic) {
        return prefix + "-" + topic;
    }

    // ========== Seckill Voucher ==========

    @Bean
    public TopicExchange seckillVoucherExchange() {
        return new TopicExchange(x(SECKILL_VOUCHER_TOPIC));
    }

    @Bean
    public Queue seckillVoucherQueue() {
        return new Queue(x(SECKILL_VOUCHER_TOPIC) + ".queue", true);
    }

    @Bean
    public Binding seckillVoucherBinding() {
        return BindingBuilder.bind(seckillVoucherQueue()).to(seckillVoucherExchange()).with("#");
    }

    // ========== Cache Invalidation ==========

    @Bean
    public TopicExchange cacheInvalidationExchange() {
        return new TopicExchange(x(SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC));
    }

    @Bean
    public Queue cacheInvalidationQueue() {
        return new Queue(x(SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC) + ".queue", true);
    }

    @Bean
    public Binding cacheInvalidationBinding() {
        return BindingBuilder.bind(cacheInvalidationQueue()).to(cacheInvalidationExchange()).with("#");
    }

    // ========== Cache Invalidation DLQ ==========

    @Bean
    public TopicExchange cacheInvalidationDlqExchange() {
        return new TopicExchange(x(SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC) + ".DLQ");
    }

    @Bean
    public Queue cacheInvalidationDlqQueue() {
        return new Queue(x(SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC) + ".DLQ.queue", true);
    }

    @Bean
    public Binding cacheInvalidationDlqBinding() {
        return BindingBuilder.bind(cacheInvalidationDlqQueue()).to(cacheInvalidationDlqExchange()).with("#");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
