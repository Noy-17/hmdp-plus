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

import static org.javaup.constant.Constant.SHOP_SYNC_TOPIC;

@Configuration
public class RabbitMQConfig {

    @Value("${prefix.distinction.name:hmdp}")
    private String prefix;

    private String x(String topic) {
        return prefix + "-" + topic;
    }

    @Bean
    public TopicExchange shopSyncExchange() {
        return new TopicExchange(x(SHOP_SYNC_TOPIC));
    }

    @Bean
    public Queue shopSyncQueue() {
        return new Queue(x(SHOP_SYNC_TOPIC) + ".queue", true);
    }

    @Bean
    public Binding shopSyncBinding() {
        return BindingBuilder.bind(shopSyncQueue()).to(shopSyncExchange()).with("#");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
