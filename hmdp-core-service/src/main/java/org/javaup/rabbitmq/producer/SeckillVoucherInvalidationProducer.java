package org.javaup.rabbitmq.producer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.AbstractProducerHandler;
import org.javaup.message.MessageExtend;
import org.javaup.rabbitmq.message.SeckillVoucherInvalidationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SeckillVoucherInvalidationProducer extends AbstractProducerHandler<MessageExtend<SeckillVoucherInvalidationMessage>> {

    private static final String RETRY_COUNT = "retryCount";

    private static final String DLQ = ".DLQ";

    @Resource
    private MeterRegistry meterRegistry;

    @Value("${seckill.cache.invalidate.retry.maxAttempts:3}")
    private int retryMaxAttempts;

    @Value("${seckill.cache.invalidate.retry.initialBackoffMillis:200}")
    private long initialBackoffMillis;

    @Value("${seckill.cache.invalidate.retry.maxBackoffMillis:800}")
    private long maxBackoffMillis;

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    public SeckillVoucherInvalidationProducer(final RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }

    @Override
    protected void afterSendFailure(final String exchange, final MessageExtend<SeckillVoucherInvalidationMessage> message, final Throwable throwable) {
        final SeckillVoucherInvalidationMessage body = message.getMessageBody();
        final Long voucherId = body.getVoucherId();
        final String reason = body.getReason();
        final String errMsg = throwable == null ? "unknown" : throwable.getMessage();
        log.error("SeckillVoucherInvalidation send failed, exchange={}, uuid={}, key={}, voucherId={}, reason={}, error= {}",
                exchange, message.getUuid(), message.getKey(), voucherId, reason, errMsg, throwable);
        if (exchange.contains(DLQ)) {
            safeInc("seckill_invalidation_dlq", "reason", "send_failures");
            return;
        } else {
            safeInc("seckill_invalidation_send_failures", "topic", exchange);
        }

        Map<String, String> headers = message.getHeaders();
        headers = headers == null ? new HashMap<>(8) : new HashMap<>(headers);
        int retryCount = 0;
        try {
            if (headers.containsKey(RETRY_COUNT)) {
                retryCount = Integer.parseInt(headers.get(RETRY_COUNT));
            }
        } catch (Exception ignore) {
        }

        if (retryCount < retryMaxAttempts) {
            long backoff = Math.min(initialBackoffMillis * (1L << retryCount), maxBackoffMillis);
            headers.put(RETRY_COUNT, String.valueOf(retryCount + 1));
            headers.put("lastError", truncate(errMsg));
            message.setHeaders(headers);
            log.warn("Retry sending cache invalidation, exchange={}, uuid={}, voucherId={}, retryCount={}, backoffMs={}",
                    exchange, message.getUuid(), voucherId, retryCount + 1, backoff);
            safeInc("seckill_invalidation_send_retries", "topic", exchange);
            sleepQuietly(backoff);
            sendRecord(exchange, message);
            return;
        }

        final String dlqReason = "send_invalid_cache_broadcast_failed: " + truncate(errMsg);
        try {
            sendToDlq(exchange, body, dlqReason);
            log.warn("Send cache invalidation to DLQ, originalExchange={}, uuid={}, voucherId={}, dlqReason={}",
                    exchange, message.getUuid(), voucherId, dlqReason);
            auditLog.warn("DLQ_PUBLISH|exchange={}|uuid={}|key={}|voucherId={}|reason={}",
                    exchange, message.getUuid(), message.getKey(), voucherId, dlqReason);
            safeInc("seckill_invalidation_send_dlq", "topic", exchange);
        } catch (Exception e) {
            log.error("Send cache invalidation to DLQ failed, originalExchange={}, uuid={}, voucherId={}, error={}",
                    exchange, message.getUuid(), voucherId, e.getMessage(), e);
            safeInc("seckill_invalidation_send_dlq_failures", "topic", exchange);
        }
    }

    @Override
    protected void afterSendSuccess(final String exchange, final MessageExtend<SeckillVoucherInvalidationMessage> message) {
        super.afterSendSuccess(exchange, message);
        boolean dlqReplay = message != null && message.getHeaders() != null && "1".equals(message.getHeaders().getOrDefault("dlqReplayCount", "0"));
        safeInc("seckill_invalidation_send_success", "topic", exchange);
        if (dlqReplay) {
            safeInc("seckill_invalidation_dlq_replay_success", "topic", exchange);
            auditLog.info("DLQ_REPLAY_SUCCESS|exchange={}|uuid={}|key={}|voucherId={}",
                    exchange, message.getUuid(), message.getKey(), message.getMessageBody().getVoucherId());
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 256 ? s : s.substring(0, 256);
    }

    private void sleepQuietly(long backoffMs) {
        try {
            TimeUnit.MILLISECONDS.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
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
