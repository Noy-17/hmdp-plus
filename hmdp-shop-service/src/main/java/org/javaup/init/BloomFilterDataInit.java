package org.javaup.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.entity.Shop;
import org.javaup.handler.BloomFilterHandlerFactory;
import org.javaup.service.IShopService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.javaup.constant.Constant.BLOOM_FILTER_HANDLER_SHOP;

/**
 * 启动时将所有商铺 ID 加载到布隆过滤器，拦截非法 ID 请求，防止缓存穿透。
 */
@Slf4j
@Order(1)
@Component
public class BloomFilterDataInit {

    @Resource
    private IShopService shopService;

    @Resource
    private BloomFilterHandlerFactory bloomFilterHandlerFactory;

    @PostConstruct
    public void init() {
        log.info("==========初始化商铺的布隆过滤器==========");
        List<Shop> shopList = shopService.list();
        for (Shop shop : shopList) {
            bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_SHOP).add(String.valueOf(shop.getId()));
        }
    }
}
