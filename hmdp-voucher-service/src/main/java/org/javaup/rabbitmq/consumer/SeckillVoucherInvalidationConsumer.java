package org.javaup.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.cache.SeckillVoucherLocalCache;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.core.RedisKeyManage;
import org.javaup.message.MessageExtend;
import org.javaup.rabbitmq.message.SeckillVoucherInvalidationMessage;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.servicelock.LockType;
import org.javaup.servicelock.annotion.ServiceLock;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.aop.framework.AopContext;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.javaup.constant.Constant.SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC;
import static org.javaup.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;
import static org.javaup.constant.DistributedLockConstants.UPDATE_SECKILL_VOUCHER_LOCK;

@Slf4j
@Component
public class SeckillVoucherInvalidationConsumer extends AbstractConsumerHandler<SeckillVoucherInvalidationMessage> {

    @Resource
    private SeckillVoucherLocalCache seckillVoucherLocalCache;

    @Resource
    private MeterRegistry meterRegistry;

    @Resource
    private RedisCache redisCache;

    public SeckillVoucherInvalidationConsumer() {
        super(SeckillVoucherInvalidationMessage.class);
    }

    @RabbitListener(queues = SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC + ".queue")
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
            log.warn("收到缓存失效消息但载荷为空或voucherId缺失, uuid={}", message.getUuid());
            return;
        }
        Long voucherId = body.getVoucherId();

        ((SeckillVoucherInvalidationConsumer) AopContext.currentProxy()).delCache(voucherId);
    }

    @ServiceLock(lockType = LockType.Write, name = UPDATE_SECKILL_VOUCHER_LOCK, keys = {"#voucherId"})
    public void delCache(Long voucherId) {
        RedisKeyBuild seckillVoucherRedisKey =
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId);
        seckillVoucherLocalCache.invalidate(seckillVoucherRedisKey.getRelKey());

        redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId));
        redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId));
        redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_NULL_TAG_KEY, voucherId));
    }

    @Override
    protected void afterConsumeFailure(final MessageExtend<SeckillVoucherInvalidationMessage> message, final Throwable throwable) {
        super.afterConsumeFailure(message, throwable);
        log.warn("删除Redis缓存失败 voucherId={}", message.getMessageBody().getVoucherId(), throwable);
        safeInc(errorTag(throwable));
    }

    private void safeInc(String tagValue) {
        try {
            if (meterRegistry != null) {
                meterRegistry.counter("seckill_invalidation_consume_failures", "error", tagValue).increment();
            }
        } catch (Exception ignore) {
        }
    }

    private String errorTag(Throwable t) {
        return t == null ? "unknown" : t.getClass().getSimpleName();
    }
}
