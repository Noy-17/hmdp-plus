package org.javaup.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.core.RedisKeyManage;
import org.javaup.entity.Shop;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.IShopService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时清空所有商铺相关的 Redis 缓存，确保服务重启后缓存状态干净、一致性可控。
 */
@Slf4j
@Order(3)
@Component
public class DelCacheDataInit {

    @Resource
    private RedisCache redisCache;

    @Resource
    private IShopService shopService;

    @PostConstruct
    public void init(){
        log.info("==========删除商铺缓存中的数据==========");
        List<Shop> shopList = shopService.list();
        for (final Shop shop : shopList) {
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY, shop.getId()));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY_NULL, shop.getId()));
        }
    }
}
