package org.javaup;


import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.message.MessageExtend;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * RabbitMQ 消息发送抽象处理器。
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractProducerHandler<M extends MessageExtend<?>> {

    private final RabbitTemplate rabbitTemplate;

    public final void sendMqMessage(String exchange, M message) {
        Assert.hasText(exchange, "exchange must not be blank");
        Assert.notNull(message, "message must not be null");
        try {
            rabbitTemplate.convertAndSend(exchange, "", message, msg -> {
                Map<String, String> headers = message.getHeaders();
                if (headers != null && !headers.isEmpty()) {
                    headers.forEach((k, v) -> {
                        if (Objects.nonNull(k) && Objects.nonNull(v)) {
                            msg.getMessageProperties().setHeader(k, v);
                        }
                    });
                }
                return msg;
            });
            afterSendSuccess(exchange, message);
        } catch (Exception ex) {
            afterSendFailure(exchange, message, ex);
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> void sendPayload(String exchange, T payload) {
        M message = (M) MessageExtend.of(payload);
        sendMqMessage(exchange, message);
    }

    @SuppressWarnings("unchecked")
    public final <T> void sendPayload(String exchange, String routingKey, T payload, Map<String, String> headers) {
        M message = (M) MessageExtend.of(payload, routingKey, headers);
        sendRecord(exchange, message);
    }

    public final void sendRecord(String exchange, M message) {
        Assert.hasText(exchange, "exchange must not be blank");
        Assert.notNull(message, "message must not be null");

        try {
            rabbitTemplate.convertAndSend(exchange, message.getKey(), message, msg -> {
                Map<String, String> headers = message.getHeaders();
                if (headers != null && !headers.isEmpty()) {
                    headers.forEach((k, v) -> {
                        if (Objects.nonNull(k) && Objects.nonNull(v)) {
                            msg.getMessageProperties().setHeader(k, v);
                        }
                    });
                }
                return msg;
            });
            afterSendSuccess(exchange, message);
        } catch (Exception ex) {
            afterSendFailure(exchange, message, ex);
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> void sendBatch(String exchange, List<T> payloads) {
        Assert.hasText(exchange, "exchange must not be blank");
        Assert.notNull(payloads, "payloads must not be null");
        for (T payload : payloads) {
            M message = (M) MessageExtend.of(payload);
            sendMqMessage(exchange, message);
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> void sendAndWait(String exchange, T payload) throws ExecutionException, InterruptedException {
        M message = (M) MessageExtend.of(payload);
        rabbitTemplate.invoke(t -> {
            t.convertAndSend(exchange, "", message);
            return null;
        });
        afterSendSuccess(exchange, message);
    }

    @SuppressWarnings("unchecked")
    public final <T> void sendToDlq(String originalExchange, T payload, String reason) {
        String dlqExchange = originalExchange + ".DLQ";
        M message = (M) MessageExtend.of(payload);
        message.setHeaders(Map.of("dlqReason", reason));
        sendRecord(dlqExchange, message);
    }

    protected void afterSendSuccess(String exchange, M message) {
        log.info("rabbitmq message send success, exchange={}, uuid={}", exchange, message.getUuid());
    }

    protected void afterSendFailure(String exchange, M message, Throwable throwable) {
        log.error("rabbitmq message send failed, exchange={}, message={}", exchange, JSON.toJSON(message), throwable);
    }
}
