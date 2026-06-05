package org.javaup.rabbitmq.consumer;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.message.MessageExtend;
import org.javaup.rabbitmq.message.SeckillVoucherInvalidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.javaup.constant.Constant.SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC;
import static org.javaup.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

@Slf4j
@Component
public class SeckillVoucherInvalidationDlqConsumer extends AbstractConsumerHandler<SeckillVoucherInvalidationMessage> {

    @Resource
    private MeterRegistry meterRegistry;

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    public SeckillVoucherInvalidationDlqConsumer() {
        super(SeckillVoucherInvalidationMessage.class);
    }

    @RabbitListener(queues = SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC + ".DLQ.queue")
    public void onMessage(Message amqpMsg, Channel channel,
                          @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) throws Exception {
        String value = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> headers = amqpMsg.getMessageProperties().getHeaders();
        consumeRaw(value, routingKey, headers);
        channel.basicAck(amqpMsg.getMessageProperties().getDeliveryTag(), false);
    }

    @Override
    protected void doConsume(MessageExtend<SeckillVoucherInvalidationMessage> message) {
        SeckillVoucherInvalidationMessage body = message.getMessageBody();
        if (Objects.isNull(body.getVoucherId())) {
            log.warn("DLQ消息载荷为空或voucherId缺失, uuid={}", message.getUuid());
            safeInc("seckill_invalidation_dlq_replay_skipped", "reason", "invalid_payload");
            return;
        }

        safeInc("seckill_invalidation_dlq", "reason", "invalid_payload");
        auditLog.error("SECKILL_INVALIDATION_DLQ | message={}", JSON.toJSONString(message));
    }

    private void safeInc(String name, String tagKey, String tagValue) {
        try {
            if (meterRegistry != null) {
                meterRegistry.counter(name, tagKey, tagValue).increment();
            }
        } catch (Exception ignore) {
        }
    }
}
