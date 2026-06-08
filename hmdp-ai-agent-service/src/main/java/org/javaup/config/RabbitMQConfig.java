package org.javaup.config;

import org.javaup.constant.Constant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${" + Constant.PREFIX_DISTINCTION_NAME + ":" + Constant.DEFAULT_PREFIX_DISTINCTION_NAME + "}")
    private String prefix;

    private String x(String topic) {
        return prefix + "-" + topic;
    }

    @Bean
    public TopicExchange userBehaviorExchange() {
        return new TopicExchange(x(Constant.USER_BEHAVIOR_TOPIC));
    }

    @Bean
    public Queue userBehaviorQueue() {
        return new Queue(x(Constant.USER_BEHAVIOR_TOPIC) + ".queue", true);
    }

    @Bean
    public Binding userBehaviorBinding() {
        return BindingBuilder.bind(userBehaviorQueue())
                .to(userBehaviorExchange())
                .with("#");
    }
}
