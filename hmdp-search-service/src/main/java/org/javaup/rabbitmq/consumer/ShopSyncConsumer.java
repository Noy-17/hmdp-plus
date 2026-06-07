package org.javaup.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.entity.Shop;
import org.javaup.mapper.ShopMapper;
import org.javaup.message.MessageExtend;
import org.javaup.rabbitmq.message.ShopSyncMessage;
import org.javaup.service.ShopSearchService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.javaup.constant.Constant.SHOP_SYNC_TOPIC;
import static org.javaup.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

@Slf4j
@Component
public class ShopSyncConsumer extends AbstractConsumerHandler<ShopSyncMessage> {

    @Resource
    private ShopSearchService shopSearchService;

    @Resource
    private ShopMapper shopMapper;

    public ShopSyncConsumer() {
        super(ShopSyncMessage.class);
    }

    @RabbitListener(queues = SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SHOP_SYNC_TOPIC + ".queue")
    public void onMessage(Message amqpMsg, Channel channel,
                          @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) throws Exception {
        String value = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> headers = amqpMsg.getMessageProperties().getHeaders();
        consumeRaw(value, routingKey, headers);
        channel.basicAck(amqpMsg.getMessageProperties().getDeliveryTag(), false);
    }

    @Override
    protected void doConsume(MessageExtend<ShopSyncMessage> message) {
        ShopSyncMessage body = message.getMessageBody();
        Long shopId = body.getShopId();
        String operation = body.getOperation();
        log.debug("Shop sync: operation={}, shopId={}", operation, shopId);

        switch (operation) {
            case "CREATE":
            case "UPDATE":
                Shop shop = shopMapper.selectById(shopId);
                if (shop != null) {
                    shopSearchService.syncFromShop(shop);
                } else {
                    log.warn("Shop sync: shop not found in DB, shopId={}", shopId);
                }
                break;
            case "DELETE":
                shopSearchService.deleteFromEs(shopId);
                break;
            default:
                log.warn("Shop sync: unknown operation={}", operation);
        }
    }
}
