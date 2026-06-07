package org.javaup.rabbitmq.producer;

import lombok.extern.slf4j.Slf4j;
import org.javaup.AbstractProducerHandler;
import org.javaup.message.MessageExtend;
import org.javaup.rabbitmq.message.ShopSyncMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShopSyncProducer extends AbstractProducerHandler<MessageExtend<ShopSyncMessage>> {

    public ShopSyncProducer(RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }
}
