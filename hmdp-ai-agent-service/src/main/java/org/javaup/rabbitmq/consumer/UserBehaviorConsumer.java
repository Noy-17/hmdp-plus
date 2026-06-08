package org.javaup.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.dto.UserBehaviorEvent;
import org.javaup.entity.UserBehavior;
import org.javaup.mapper.UserBehaviorMapper;
import org.javaup.message.MessageExtend;
import org.javaup.service.UserProfileService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.javaup.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;
import static org.javaup.constant.Constant.USER_BEHAVIOR_TOPIC;

@Slf4j
@Component
public class UserBehaviorConsumer extends AbstractConsumerHandler<UserBehaviorEvent> {

    private final UserBehaviorMapper userBehaviorMapper;
    private final UserProfileService userProfileService;

    public UserBehaviorConsumer(UserBehaviorMapper userBehaviorMapper,
                                UserProfileService userProfileService) {
        super(UserBehaviorEvent.class);
        this.userBehaviorMapper = userBehaviorMapper;
        this.userProfileService = userProfileService;
    }

    @RabbitListener(queues = SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + USER_BEHAVIOR_TOPIC + ".queue")
    public void onMessage(Message amqpMsg, Channel channel,
                          @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey) throws Exception {
        String value = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> headers = amqpMsg.getMessageProperties().getHeaders();
        consumeRaw(value, routingKey, headers);
        channel.basicAck(amqpMsg.getMessageProperties().getDeliveryTag(), false);
    }

    @Override
    protected void doConsume(MessageExtend<UserBehaviorEvent> message) {
        UserBehaviorEvent event = message.getMessageBody();
        if (event == null) {
            return;
        }

        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(event.getUserId());
        behavior.setEventType(event.getEventType());
        behavior.setTargetId(event.getTargetId());
        behavior.setTargetType(event.getTargetType());
        behavior.setEventTimestamp(event.getTimestamp() != null ? event.getTimestamp() : System.currentTimeMillis());
        behavior.setCreateTime(LocalDateTime.now());
        userBehaviorMapper.insert(behavior);

        userProfileService.updateProfile(event);
    }
}
