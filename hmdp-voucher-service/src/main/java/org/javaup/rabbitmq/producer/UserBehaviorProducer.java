package org.javaup.rabbitmq.producer;

import org.javaup.AbstractProducerHandler;
import org.javaup.dto.UserBehaviorEvent;
import org.javaup.message.MessageExtend;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.javaup.constant.Constant.DEFAULT_PREFIX_DISTINCTION_NAME;
import static org.javaup.constant.Constant.PREFIX_DISTINCTION_NAME;
import static org.javaup.constant.Constant.USER_BEHAVIOR_TOPIC;

@Component
public class UserBehaviorProducer extends AbstractProducerHandler<MessageExtend<UserBehaviorEvent>> {

    @Value("${" + PREFIX_DISTINCTION_NAME + ":" + DEFAULT_PREFIX_DISTINCTION_NAME + "}")
    private String prefix;

    public UserBehaviorProducer(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    public void send(UserBehaviorEvent event) {
        sendPayload(prefix + "-" + USER_BEHAVIOR_TOPIC, event);
    }
}
