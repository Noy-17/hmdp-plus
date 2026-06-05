package org.javaup.rabbitmq.producer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.AbstractProducerHandler;
import org.javaup.enums.SeckillVoucherOrderOperate;
import org.javaup.message.MessageExtend;
import org.javaup.rabbitmq.message.SeckillVoucherMessage;
import org.javaup.rabbitmq.redis.RedisVoucherData;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillVoucherProducer extends AbstractProducerHandler<MessageExtend<SeckillVoucherMessage>> {

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private RedisVoucherData redisVoucherData;

    public SeckillVoucherProducer(final RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    protected void afterSendFailure(final String exchange, final MessageExtend<SeckillVoucherMessage> message, final Throwable throwable) {
        super.afterSendFailure(exchange, message, throwable);
        long traceId = snowflakeIdGenerator.nextId();
        redisVoucherData.rollbackRedisVoucherData(
                SeckillVoucherOrderOperate.YES,
                traceId,
                message.getMessageBody().getVoucherId(),
                message.getMessageBody().getUserId(),
                message.getMessageBody().getOrderId(),
                message.getMessageBody().getAfterQty(),
                message.getMessageBody().getChangeQty(),
                message.getMessageBody().getBeforeQty());
    }
}
